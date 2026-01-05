package dk.skancode.skanmate.barcode

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

class BoundingBoxGraphic(
    overlay: GraphicOverlay,
    private val rectFactory: (width: Float, height: Float) -> Rect,
): Graphic(overlay) {
    val rect: Rect
        get() = rectFactory(imageWidth, imageHeight)

    override fun DrawScope.draw() {
        val mappedRect = translateRect(rect)

        drawRoundRect(
            color = Color.White,
            topLeft = mappedRect.topLeft,
            size = mappedRect.size,
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(4.0f),
        )
    }

    fun contains(offset: Offset): Boolean {
        return this.rect.contains(offset)
    }

    fun contains(offsets: List<Offset>): Boolean {
        return offsets.all { offset -> this.contains(offset) }
    }

    fun contains(rect: Rect): Boolean {
        return contains(listOf(rect.topLeft, rect.bottomRight))
    }
}

fun Canvas.drawRoundRect(rect: Rect, radiusX: Float, radiusY: Float, paint: Paint) {
    this.drawRoundRect(
        left = rect.left,
        right = rect.right,
        top = rect.top,
        bottom = rect.bottom,
        radiusX = radiusX,
        radiusY = radiusY,
        paint = paint,
    )
}