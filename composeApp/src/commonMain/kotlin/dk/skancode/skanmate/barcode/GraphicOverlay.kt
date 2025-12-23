package dk.skancode.skanmate.barcode

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope

interface GraphicOverlay {
    val overlayWidth: Int
    val overlayHeight: Int
    val imageSize: Size

    val transformationMatrix: Matrix
    val postScaleWidthOffset: Float
    val postScaleHeightOffset: Float
    val scaleFactor: Float
    val isImageFlipped: Boolean


    /** Removes all graphics from the overlay.  */
    fun clear()

    /** Adds a graphic to the overlay.  */
    fun addGraphic(graphic: Graphic)

    /** Adds a graphic to the overlay.  */
    fun add(graphic: Graphic)

    /** Removes a graphic from the overlay.  */
    fun remove(graphic: Graphic)

    /**
     * Sets the source information of the image being processed by detectors, including size and
     * whether it is flipped, which informs how to transform image coordinates later.
     *
     * @param imageWidth the width of the image sent to ML Kit detectors
     * @param imageHeight the height of the image sent to ML Kit detectors
     * @param isFlipped whether the image is flipped. Should set it to true when the image is from the
     * front camera.
     */
    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, isFlipped: Boolean)

    fun drawOverlay(scope: DrawScope)
    fun drawOverlay(scope: DrawScope, token: Long)
}