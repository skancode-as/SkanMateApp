package dk.skancode.skanmate.barcode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope

abstract class Graphic(private val overlay: GraphicOverlay) {

    /**
     * Returns a [Matrix] for transforming from image coordinates to overlay view coordinates.
     */
    val transformationMatrix: Matrix
        get() = overlay.transformationMatrix

    val imageSize: Size
        get() = overlay.imageSize
    val imageWidth: Float
        get() = imageSize.width
    val imageHeight: Float
        get() = imageSize.height

    /**
     * Draw the graphic on the supplied canvas. Drawing should use the following methods to convert
     * to view coordinates for the graphics that are drawn:
     *
     *
     *  1. [Graphic.scale] adjusts the size of the supplied value from the image
     * scale to the view scale.
     *  1. [Graphic.translateX] and [Graphic.translateY] adjust the
     * coordinate from the image's coordinate system to the view coordinate system.
     */
    abstract fun DrawScope.draw()

    /**
     * Adjusts the supplied value from the image scale to the view scale.
     */
    fun scale(imagePixel: Float): Float {
        return imagePixel * overlay.scaleFactor
    }

    fun isImageFlipped(): Boolean {
        return overlay.isImageFlipped
    }

    /**
     * Adjusts the x coordinate from the image's coordinate system to the view coordinate system.
     */
    fun translateX(x: Float): Float {
        return if (overlay.isImageFlipped) {
            overlay.overlayWidth - (scale(x) - overlay.postScaleWidthOffset)
        } else {
            scale(x) - overlay.postScaleWidthOffset
        }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    fun translateY(y: Float): Float {
        return scale(y) - overlay.postScaleHeightOffset
    }

    fun translatePoint(point: Offset): Offset {
        return Offset(
            x = translateX(x = point.x),
            y = translateY(y = point.y),
        )
    }

    fun translatePoints(points: List<Offset>): List<Offset> {
        return points.map { translatePoint(point = it) }
    }

    fun translateRect(rect: Rect): Rect {
        return Rect(
            topLeft = translatePoint(rect.topLeft),
            bottomRight = translatePoint(rect.bottomRight),
        )
    }

    fun mapRect(rect: Rect): Rect {
        return transformationMatrix.map(rect)
    }

    fun mapPoint(point: Offset): Offset {
        return transformationMatrix.map(point)
    }

    fun mapPoints(points: List<Offset>): List<Offset> =
        points.map { point -> mapPoint(point) }

    fun mapPoints(points: FloatArray): FloatArray {
        if (points.isEmpty()) return points

        val offsets = mutableListOf<Offset>()
        for (i in 0..<points.size step 2) {
            offsets.add(Offset(points[i], points[i+1]))
        }

        val mapped = mapPoints(offsets).flatMap { offset ->
            listOf(offset.x, offset.y)
        }
        return FloatArray(mapped.size) { i -> mapped[i] }
    }
}
