package dk.skancode.skanmate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.UIKitViewController
import dk.skancode.skanmate.ui.component.barcode.BarcodeBoundingBox
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.ui.component.barcode.BarcodeInfo
import dk.skancode.skanmate.ui.component.barcode.BarcodeResult
import dk.skancode.skanmate.util.Success
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.position
import kotlin.collections.emptyList

@Composable
fun IosCameraScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    scannerController: ScannerController,
    result: (BarcodeResult) -> Unit
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
            onBarcodes = {
                if (it.isNotEmpty()) {
                    result(BarcodeResult.OnSuccess(barcodes = it))
                }
            },
            onError = {
                result(BarcodeResult.OnFailed(Exception("Unknown")))
            }
        )
    }

    scannerController.captureDevice = device

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current

        val (leftInset, rightInset) = remember(density, layoutDirection) {
            val insets = WindowInsets()

            val leftToRight = insets.getLeft(density, layoutDirection) to insets.getRight(
                density,
                layoutDirection
            )

            leftToRight
        }
        val windowSize = LocalWindowInfo.current.containerSize

        val cameraViewWidthPx = windowSize.width - (rightInset + leftInset)
        val cameraViewHeightPx = cameraViewWidthPx * 5f / 4f
        val cameraViewWidthDp = with(density) { cameraViewWidthPx.toDp() }
        val cameraViewHeightDp = with(density) { cameraViewHeightPx.toDp() }
        Box(
            modifier = modifier.fillMaxWidth().wrapContentHeight(),
            propagateMinConstraints = true,
        ) {
            UIKitViewController(
                factory = { cameraUiKitViewController },
                modifier = Modifier
                    .size(
                        width = cameraViewWidthDp,
                        height = cameraViewHeightDp
                    )
                    .fillMaxSize(),
            )
        }
    }
}