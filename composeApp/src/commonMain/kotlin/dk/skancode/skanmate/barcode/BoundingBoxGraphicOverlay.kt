package dk.skancode.skanmate.barcode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

class BoundingBoxGraphicOverlay: BaseGraphicOverlay() {
    val boundingBoxGraphic = BoundingBoxGraphic(this) { width, height ->
        val center = Offset(
            x = width / 2f,
            y = height / 2f,
        )
        val boxWidth = min(width * 0.8f, height * 0.8f)
        val size = Size(
            width = boxWidth,
            height = boxWidth,
        )

        Rect(
            offset = Offset(
                x = center.x - size.width / 2f,
                y = center.y - size.height / 2f,
            ),
            size = size,
        )
    }

    override fun drawOverlay(scope: DrawScope) {
        addGraphic(boundingBoxGraphic)

        super.drawOverlay(scope)
    }
}