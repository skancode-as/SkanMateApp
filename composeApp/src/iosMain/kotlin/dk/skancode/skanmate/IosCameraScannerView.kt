package dk.skancode.skanmate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import dk.skancode.skanmate.barcode.BarcodeProcessorBase
import dk.skancode.skanmate.barcode.BaseGraphicOverlay
import dk.skancode.skanmate.barcode.GraphicOverlay
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.util.rememberCameraViewSize
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.position

@Composable
fun IosCameraScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    scannerController: ScannerController,
    barcodeOverlay: BaseGraphicOverlay,
    processor: BarcodeProcessorBase<List<BarcodeData>>
) {
    val device: AVCaptureDevice? = AVCaptureDevice
        .devicesWithMediaType(AVMediaTypeVideo)
        .firstOrNull { device ->
            (device as AVCaptureDevice).position == AVCaptureDevicePositionBack
        } as AVCaptureDevice?
    if (device == null) {
        println("Back camera not available")
        return
    }

    val cameraUiKitViewController = remember {
        CameraUiKitViewController(
            device = device,
            codeTypes = codeTypes,
            onBarcodes = { result ->
                barcodeOverlay.clear()
                processor.onSuccess(result = result, graphicOverlay = barcodeOverlay)
            },
            onError = {
                processor.onFailure(Exception("Unknown"))
            },
            graphicOverlay = barcodeOverlay,
        )
    }

    scannerController.captureDevice = device

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = modifier.fillMaxWidth().wrapContentHeight(), propagateMinConstraints = true) {
            val cameraViewSize by rememberCameraViewSize()
            UIKitViewController(
                factory = { cameraUiKitViewController },
                modifier = Modifier
                    .size(cameraViewSize)
                    .align(Alignment.Center),
            )
            GraphicOverlay(
                modifier = Modifier
                    .size(cameraViewSize)
                    .align(Alignment.Center),
                overlay = barcodeOverlay,
            )
        }
    }
}