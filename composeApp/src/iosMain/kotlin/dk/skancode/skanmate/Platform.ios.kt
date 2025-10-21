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
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.skia.Image
import org.jetbrains.skia.makeFromEncoded
import platform.Foundation.*
import platform.posix.memcpy

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
        )[0] as? String
    }

    LaunchedEffect(fileManager, documentDir) {
        if (documentDir != null && fileManager.fileExistsAtPath("$documentDir/$imagePath")) {
            val data: NSData = NSData.create(contentsOfFile = "$documentDir/$imagePath")!!

            val imageBitmap = try {
                Image.makeFromEncoded(data).toComposeImageBitmap()
            } catch (e: Exception) {
                println(e)
                null
            }
            if (imageBitmap != null) {
                stateFlow.update { imageBitmap }
            }
        }
    }

    val bitmap = stateFlow.collectAsState()
    painterState.value = BitmapPainter(bitmap.value)

    return painterState
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    return ByteArray(length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), bytes, length)
        }
    }
}