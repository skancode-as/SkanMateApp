package dk.skancode.skanmate.ui.component.barcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.util.OnSuccess
import dk.skancode.skanmate.util.Success

@Composable
fun CameraBarcodeBoundingBox(
    modifier: Modifier = Modifier,
    barcodeData: List<BarcodeData>,
    cameraViewSize: DpSize,
    onSelect: (BarcodeData) -> Unit,
) {
    println("CameraBarcodeBoundingBox - barcodeData: $barcodeData")

    val cameraViewWidthDp = cameraViewSize.width
    val cameraViewHeightDp = cameraViewSize.height
    var clickedOffset by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelMedium.copy(color = Color.OnSuccess)
    Canvas(modifier = modifier
        .border(1.dp, Color.Cyan)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                clickedOffset = offset
            }
        },
    ) {
        fun Offset.toCanvasSpace(): Offset {
            return Offset(
                x = this.x * size.width,
                y = this.y * size.height,
            )
        }


        repeat(barcodeData.size) { i ->
            val data = barcodeData[i]
            val box = data.box

            val topLeft = box.topLeft.toCanvasSpace()
            val botRight = box.botRight.toCanvasSpace()
            val rectSize = Size(
                width =  ((botRight.x - box.topLeft.x) / cameraViewWidthDp.value)  * size.width,
                height = ((botRight.y - box.topLeft.y) / cameraViewHeightDp.value) * size.height,
            )
            val clickedOffset = clickedOffset
            if (clickedOffset != null) {
                if ((topLeft.x <= clickedOffset.x && clickedOffset.x <= topLeft.x + rectSize.width) &&
                    (topLeft.y <= clickedOffset.y && clickedOffset.y <= topLeft.y + rectSize.height)) {

                    onSelect(data)
                }
            }

            val points = listOf(
                box.topLeft.toCanvasSpace(), box.topRight.toCanvasSpace(),
                box.topRight.toCanvasSpace(), box.botRight.toCanvasSpace(),
                box.botRight.toCanvasSpace(), box.botLeft.toCanvasSpace(),
                box.botLeft.toCanvasSpace(), box.topLeft.toCanvasSpace(),
            )

            println("CameraBarcodeBoundingBox - points: $points")

            drawPoints(
                points = points,
                pointMode = PointMode.Lines,
                color = Color.Success,
                strokeWidth = 15f,
                cap = StrokeCap.Round,
                alpha = 0.5f,
            )

            val textResult = textMeasurer.measure(data.info.value, textStyle)
            val botLeft = box.botLeft.toCanvasSpace()

            drawRect(
                color = Color.Success,
                topLeft = botLeft,
                size = Size(textResult.size.width.toFloat(), textResult.size.height.toFloat())
            )

            drawText(
                textLayoutResult = textResult,
                topLeft = botLeft,
                color = Color.OnSuccess
            )
        }
    }

}