package dk.skancode.skanmate.barcode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import dk.skancode.skanmate.ui.component.barcode.BarcodeData

class BarcodeGraphic(
    overlay: GraphicOverlay,
    val barcode: BarcodeData,
    private val textMeasurer: TextMeasurer? = null,
    private val containerColor: Color = MARKER_COLOR,
    private val contentColor: Color = TEXT_COLOR,
): Graphic(overlay) {
    override fun DrawScope.draw() {
        val box = barcode.box
        val cornerPoints = listOf(
            box.topLeft,
            box.topRight,
            box.botRight,
            box.botLeft,
            box.topLeft,
        )
        val mappedPoints = translatePoints(cornerPoints)

        println("BarcodeGraphic::draw() - cornerPoints: $cornerPoints, mappedPoints: $mappedPoints")

        for (i in 0 ..< mappedPoints.size-1) {
            drawLine(
                color = containerColor,
                start = mappedPoints[i],
                end = mappedPoints[i+1],
                strokeWidth = STROKE_WIDTH,
            )
        }

        if (textMeasurer != null) {
            val rect = translateRect(barcode.rect)

            val textLayoutResult = textMeasurer.measure(barcode.info.value)
            val textSize = textLayoutResult.size
            val lineHeight = textSize.height + (2 * STROKE_WIDTH)
            val offset = (textSize.width - (rect.right - rect.left)) / 2
            val topLeft = Offset(
                x = rect.left - STROKE_WIDTH - offset,
                y = rect.top - lineHeight,
            )
            drawRect(
                color = containerColor,
                topLeft = topLeft,
                size = Size(
                    width = textSize.width + 2 * STROKE_WIDTH,
                    height = lineHeight,
                )
            )

            drawText(
                textLayoutResult = textLayoutResult,
                color = contentColor,
                topLeft = Offset(
                    x = topLeft.x + STROKE_WIDTH,
                    y = topLeft.y + STROKE_WIDTH,
                )
            )
        }
    }
}

private val TEXT_COLOR = Color.Black
private val MARKER_COLOR = Color.Green
private const val STROKE_WIDTH: Float = 4.0f
