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
import dk.skancode.skanmate.util.Success
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.position
import kotlin.collections.emptyList

data class BarcodeData(
    val barcode: Barcode,
    val topLeft: Offset,
    val topRight: Offset,
    val botLeft: Offset,
    val botRight: Offset,
)

@Composable
fun IosCameraScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
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

    var barcodeData: List<BarcodeData> by remember { mutableStateOf(emptyList()) }

    val cameraUiKitViewController = remember {
        CameraUiKitViewController(
            device = device,
            codeTypes = codeTypes,
            onBarcodes = {
                barcodeData = it
                    .filter { result -> result.corners.size == 4 }
                    .map { res ->
                        val corners = res.corners.toMutableList().sortedBy { corner -> corner.y }
                        val topCorners = corners.slice(0..1).sortedBy { corner -> corner.x}
                        val botCorners = corners.slice(2..3).sortedBy { corner -> corner.x}
                        BarcodeData(
                            barcode = Barcode(data = res.value, format = res.type, rawBytes = res.rawBytes),
                            topLeft = topCorners[0].let { corner ->
                                    Offset(corner.x.toFloat(), corner.y.toFloat())
                                },
                            topRight = topCorners[1].let { corner ->
                                    Offset(corner.x.toFloat(), corner.y.toFloat())
                                },
                            botLeft = botCorners[0].let { corner ->
                                    Offset(corner.x.toFloat(), corner.y.toFloat())
                                },
                            botRight = botCorners[1].let { corner ->
                                    Offset(corner.x.toFloat(), corner.y.toFloat())
                                },
                        )
                    }
            },
            onError = {
                result(BarcodeResult.OnFailed(Exception("Unknown")))
            }
        )
    }

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

        var clickedOffset by remember { mutableStateOf<Offset?>(null) }
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    clickedOffset = offset
                }
            },
        ) {
            fun Offset.toCanvasSpace(): Offset {
                return Offset(
                    x = (this.x/cameraViewWidthDp.value) * size.width,
                    y = (this.y/cameraViewHeightDp.value) * size.height,
                )
            }

            repeat(barcodeData.size) { i ->
                val data = barcodeData[i]

                val topLeft = data.topLeft.toCanvasSpace()
                val rectSize = Size(
                    width = ((data .botRight.x - data.topLeft.x) / cameraViewWidthDp.value) * size.width,
                    height = ((data.botRight.y - data.topLeft.y) / cameraViewHeightDp.value) * size.height,
                )
                val clickedOffset = clickedOffset
                if (clickedOffset != null) {
                    if ((topLeft.x <= clickedOffset.x && clickedOffset.x <= topLeft.x + rectSize.width) &&
                        (topLeft.y <= clickedOffset.y && clickedOffset.y <= topLeft.y + rectSize.height)) {
                        result(
                            BarcodeResult.OnSuccess(data.barcode)
                        )
                    }
                }

                val points = listOf(
                    data.topLeft.toCanvasSpace(), data.topRight.toCanvasSpace(),
                    data.topRight.toCanvasSpace(), data.botRight.toCanvasSpace(),
                    data.botRight.toCanvasSpace(), data.botLeft.toCanvasSpace(),
                    data.botLeft.toCanvasSpace(), data.topLeft.toCanvasSpace(),
                )

                drawPoints(
                    points = points,
                    pointMode = PointMode.Lines,
                    color = Color.Success,
                    strokeWidth = 15f,
                    cap = StrokeCap.Round,
                    alpha = 0.5f,
                )
            }
        }
    }
}