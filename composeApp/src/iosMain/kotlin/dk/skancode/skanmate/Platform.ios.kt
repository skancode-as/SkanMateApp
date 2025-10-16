package dk.skancode.skanmate

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.util.CameraScanListener
import dk.skancode.skanmate.util.CameraScanManager
import dk.skancode.skanmate.util.LocalCameraScanManager
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGDataProviderCopyData
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.CoreGraphics.CGImageGetBytesPerRow
import platform.CoreGraphics.CGImageGetDataProvider
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.UIKit.UIImage

class IosScanModule(val cameraScanManager: CameraScanManager): ScanModule {
    private val cameraListeners: MutableMap<ScanEventHandler, CameraScanListener> = HashMap()

    override fun isHardwareScanner(): Boolean {
        return false
    }

    override fun registerListener(handler: ScanEventHandler) {
        val cameraScanListener = CameraScanListener { barcode, format ->
            handler.handle(ScanEvent.Barcode(
                barcode = barcode,
                barcodeType = format,
                ok = true,
            ))
        }

        cameraListeners[handler] = cameraScanListener
        cameraScanManager.registerListener(cameraScanListener)
    }

    override fun unregisterListener(handler: ScanEventHandler) {
        val cameraScanListener = cameraListeners.remove(handler) ?: return

        cameraScanManager.unregisterListener(cameraScanListener)
    }

    override fun enableScan() {
        cameraScanManager.startScanning()
    }

    override fun disableScan() {
        cameraScanManager.stopScanning()
    }

//    override fun enableGs1() { }
//
//    override fun disableGs1() { }

}

@Composable
actual fun rememberScanModule(): ScanModule {
    val cameraScanManager = LocalCameraScanManager.current

    return remember { IosScanModule(cameraScanManager) }
}

actual val platformSettingsFactory: Settings.Factory = NSUserDefaultsSettings.Factory()

@Composable
actual fun CameraView(
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    IosCameraView(
        cameraUi = cameraUi,
    )
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
@Composable
actual fun loadImageAsState(imagePath: String): State<Painter> {
    val fallbackBitmap = ImageBitmap(1, 1)
    val painterState = remember { mutableStateOf(BitmapPainter(fallbackBitmap)) }

    val stateFlow = remember { MutableStateFlow(fallbackBitmap) }

    val fileManager = remember { NSFileManager.defaultManager }
    val documentDir = remember {
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )[0] as String
    }

    LaunchedEffect(fileManager, documentDir) {
        println("$fileManager, $documentDir")
        if (documentDir != null && fileManager.fileExistsAtPath("$documentDir/$imagePath")) {
            val data: NSData = NSData.create(contentsOfFile = "$documentDir/$imagePath")!!
            println("filedata: $data")
            val uiImage = UIImage(data = data)
            println("uiImage: $uiImage")

            val ref = uiImage.CGImage
            println("uiImageRef: $ref")
            if (ref != null) {
                val width = CGImageGetWidth(ref).toInt()
                val height = CGImageGetHeight(ref).toInt()

                val bytesPerRow = CGImageGetBytesPerRow(ref)
                val refData = CGDataProviderCopyData(CGImageGetDataProvider(ref))
                val bytePointer = CFDataGetBytePtr(refData)
                val length = CFDataGetLength(refData)
                println("bytePointer: $bytePointer")
                println("byteLength: $length")

                val alphaType = when(CGImageGetAlphaInfo(ref)) {
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst,
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast -> ColorAlphaType.PREMUL
                    CGImageAlphaInfo.kCGImageAlphaFirst,
                    CGImageAlphaInfo.kCGImageAlphaLast -> ColorAlphaType.UNPREMUL
                    CGImageAlphaInfo.kCGImageAlphaNone,
                    CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst,
                    CGImageAlphaInfo.kCGImageAlphaNoneSkipLast -> ColorAlphaType.OPAQUE
                    else -> ColorAlphaType.UNKNOWN
                }

                val byteArray = ByteArray(
                    size = length.toInt(),
                    init = { index ->
                        bytePointer!![index].toByte()
                    },
                )

                CFRelease(refData)
                CGImageRelease(ref)

                val skiaColorSpace = ColorSpace.sRGB
                val colorType = ColorType.RGBA_8888

                for (i in byteArray.indices step 4) {
                    val r = byteArray[i]
                    val b = byteArray[i + 2]

                    byteArray[i] = b
                    byteArray[i + 2] = r
                }

                val skiaImage = Image.makeRaster(
                    imageInfo = ImageInfo(
                        width = width,
                        height = height,
                        colorType,
                        alphaType,
                        colorSpace = skiaColorSpace,
                    ),
                    bytes = byteArray,
                    rowBytes = bytesPerRow.toInt(),
                )

                stateFlow.update { skiaImage.toComposeImageBitmap() }
            }
        }
    }

    val bitmap = stateFlow.collectAsState()
    painterState.value = BitmapPainter(bitmap.value)

    return painterState
}