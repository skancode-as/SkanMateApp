package dk.skancode.skanmate

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.rememberTextMeasurer
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.barcode.BarcodeProcessorBase
import dk.skancode.skanmate.barcode.BarcodeProcessorImpl
import dk.skancode.skanmate.barcode.BoundingBoxGraphicOverlay
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.ui.component.barcode.BarcodeResult
import dk.skancode.skanmate.util.CameraScanListener
import dk.skancode.skanmate.util.CameraScanManager
import dk.skancode.skanmate.util.clamp
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.jetbrains.skia.makeFromEncoded
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode
import platform.AVFoundation.videoZoomFactor
import platform.Foundation.*
import platform.posix.memcpy

class IosScanModule(val cameraScanManager: CameraScanManager): ScanModule {
    private val cameraListeners: MutableMap<ScanEventHandler, CameraScanListener> = HashMap()

    override fun isHardwareScanner(): Boolean {
        return false
    }

    override fun registerListener(handler: ScanEventHandler) {
        val cameraScanListener = CameraScanListener { events ->
            handler.handleEvents(
                events.map { (barcode, format) ->
                    ScanEvent.Barcode(
                        barcode = barcode,
                        barcodeType = format,
                        ok = true,
                    )
                }
            )
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
    modifier: Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    IosCameraView(
        modifier = modifier,
        cameraUi = cameraUi,
    )
}

@OptIn(BetaInteropApi::class)
actual suspend fun loadLocalImage(imagePath: String): ImageData {
    val fileManager = NSFileManager.defaultManager
    val documentDir =
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )[0] as? String

    return if (documentDir != null && fileManager.fileExistsAtPath("$documentDir/$imagePath")) {
        val data: NSData = NSData.create(contentsOfFile = "$documentDir/$imagePath")!!

        ImageData(
            path = imagePath,
            name = imagePath,
            data = data.toByteArray(),
        )
    } else {
        ImageData(path = null, name = null, data = null)
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
@Composable
actual fun loadImage(imagePath: String?): ImageResource<Painter> {
    val imageResource = rememberImageResource()

    val fileManager = remember { NSFileManager.defaultManager }
    val documentDir = remember {
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )[0] as? String
    }

    LaunchedEffect(fileManager, documentDir, imagePath) {
        launch(Dispatchers.IO) {
            if (documentDir != null && fileManager.fileExistsAtPath("$documentDir/$imagePath")) {
                val data: NSData = NSData.create(contentsOfFile = "$documentDir/$imagePath")!!

                val imageBitmap = try {
                    Image.makeFromEncoded(data).toComposeImageBitmap()
                } catch (e: Exception) {
                    println(e)
                    imageResource.error(e.message ?: "Could not make Image bitmap from NSData")
                    return@launch
                }

                imageResource.update(BitmapPainter(imageBitmap))
            }
        }
    }

    return imageResource
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    return ByteArray(length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), bytes, length)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun deleteFile(path: String) {
    println("IOS::deleteFile")

    val manager = NSFileManager.defaultManager
    val dirPath = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )[0] as String
    val filePath = "$dirPath/$path"

    if (manager.fileExistsAtPath(filePath)) {
        if (manager.removeItemAtPath(filePath, error = null)) {
            println("File $path deleted")
        } else {
            println("Could not delete file: \"$path\"")
        }
    } else {
        println("No file at\n$filePath")
    }
}

@Composable
actual fun SkanMateScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    scannerController: ScannerController?,
    result: (BarcodeResult) -> Unit
) {
    val scannerController = scannerController ?: remember { ScannerController() }

    val textMeasurer = rememberTextMeasurer()
    val processor: BarcodeProcessorBase<List<BarcodeData>> = remember(result) {
        BarcodeProcessorImpl(
            onResult = result,
            textMeasurer = textMeasurer,
            successThreshold = 30,
            barcodeMinCount = 10,
        )
    }

    val barcodeOverlay = remember { BoundingBoxGraphicOverlay() }

    IosCameraScannerView(
        modifier = modifier,
        codeTypes = codeTypes,
        scannerController = scannerController,
        barcodeOverlay = barcodeOverlay,
        processor = processor,
    )
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class ScannerController {
    lateinit var captureDevice: AVCaptureDevice

    private var _torchEnabled by mutableStateOf(false)
    actual val torchEnabled: Boolean
        get() = _torchEnabled

    private var _zoomRatio by mutableStateOf(1f)
    actual val zoomRatio: Float
        get() = _zoomRatio

    private val minZoom: Float = 1f
    actual val maxZoomRatio: Float
        get() = captureDevice.activeFormat.videoMaxZoomFactor.toFloat()

    actual fun setTorch(enabled: Boolean) {
        _torchEnabled = enabled
        updateTorch(enabled)
    }

    actual fun setZoom(ratio: Float) {
        _zoomRatio = ratio
        updateZoom(ratio)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun updateTorch(enabled: Boolean) {
        if (captureDevice.hasTorch) {
            val prev = torchEnabled
            var locked = false
            try {
                locked = captureDevice.lockForConfiguration(null)
                if (locked) {
                    captureDevice.torchMode =
                        if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                    _torchEnabled = enabled
                }
            } catch (_: Throwable) {
                _torchEnabled = prev
            } finally {
                if (locked) {
                    captureDevice.unlockForConfiguration()
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun updateZoom(newZoom: Float) {
        if (captureDevice.lockForConfiguration(null)) {
            val actualNewZoom = newZoom.clamp(minZoom, maxZoomRatio).toDouble()

            _zoomRatio = actualNewZoom.toFloat()
            captureDevice.videoZoomFactor = actualNewZoom

            captureDevice.unlockForConfiguration()
        } else {
            throw Exception("Could not lock device for configuration")
        }
    }
}