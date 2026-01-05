package dk.skancode.skanmate.barcode

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import dk.skancode.skanmate.util.applyEach
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.roundToInt

@OptIn(ExperimentalAtomicApi::class)
abstract class BaseGraphicOverlay(): GraphicOverlay {
    private var previousInvalidateToken: AtomicLong = AtomicLong(0)
    private val _invalidateToken = mutableStateOf(0L)
    val invalidateToken: State<Long>
        get() = _invalidateToken
    private var overlaySize: Size = Size(width = Float.POSITIVE_INFINITY, height = Float.POSITIVE_INFINITY)

    override val overlayWidth: Int
        get() = overlaySize.width.roundToInt()
    override val overlayHeight: Int
        get() = overlaySize.height.roundToInt()

    private val graphics: MutableList<Graphic> = ArrayList()
    // Matrix for transforming from image coordinates to overlay view coordinates.
    //protected val androidTransformationMatrix = Matrix()
    override val transformationMatrix: Matrix = Matrix()

    protected var imageWidth: Int = 0
        private set
    protected var imageHeight: Int = 0
        private set
    override val imageSize: Size
        get() = Size(imageWidth.toFloat(), imageHeight.toFloat())
    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    private var _scaleFactor: Float = 1.0f
    override val scaleFactor: Float
        get() = _scaleFactor

    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private var _postScaleWidthOffset: Float = 0.0f
    override val postScaleWidthOffset: Float
        get() = _postScaleWidthOffset
    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private var _postScaleHeightOffset: Float = 0.0f
    override val postScaleHeightOffset: Float
        get() = _postScaleHeightOffset
    private var _isImageFlipped: Boolean = false
    override val isImageFlipped: Boolean
        get() = _isImageFlipped

    private var needUpdateTransformation: Boolean = true

    init {
//        addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
//            println("BarcodeGraphicOverlay::addOnLayoutChangeListener() - view: ${v.id}, left: $left, top: $top, right: $right, bottom: $bottom, oldLeft: $oldLeft, oldTop: $oldTop, oldRight: $oldRight, oldBottom: $oldBottom")
//            needUpdateTransformation = true
//        }
    }
    /** Removes all graphics from the overlay.  */
    override fun clear() {
        graphics.clear()

        invalidate()
    }

    override fun addGraphic(graphic: Graphic) {
        add(graphic)
    }

    /** Adds a graphic to the overlay.  */
    override fun add(graphic: Graphic) {
        graphics.add(graphic)
    }

    /** Removes a graphic from the overlay.  */
    override fun remove(graphic: Graphic) {
        graphics.remove(graphic)
        invalidate()
    }

    /**
     * Sets the source information of the image being processed by detectors, including size and
     * whether it is flipped, which informs how to transform image coordinates later.
     *
     * @param imageWidth the width of the image
     * @param imageHeight the height of the image
     * @param isFlipped whether the image is flipped. Should set it to true when the image is from the
     * front camera.
     */
    override fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, isFlipped: Boolean) {
        println("BaseGraphicOverlay::setImageSourceInfo(width: $imageWidth, height: $imageHeight, isFlipped: $isFlipped)")
        println("BaseGraphicOverlay::setImageSourceInfo() - [PRE  APPLY] needUpdateTransformation: $needUpdateTransformation")
        needUpdateTransformation = needUpdateTransformation
                || this.imageWidth != imageWidth
                || this.imageHeight != imageHeight
                || this._isImageFlipped != isFlipped

        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this._isImageFlipped = isFlipped

        println("BaseGraphicOverlay::setImageSourceInfo() - [POST APPLY] needUpdateTransformation: $needUpdateTransformation")

        if (needUpdateTransformation) {
            invalidate()
        }
    }

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return
        }
        val viewAspectRatio = overlayWidth.toFloat() / overlayHeight
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        _postScaleWidthOffset = 0f
        _postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            _scaleFactor = overlayWidth.toFloat() / imageWidth
            _postScaleHeightOffset += (overlayWidth.toFloat() / imageAspectRatio - overlayHeight) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            _scaleFactor = overlayHeight.toFloat() / imageHeight
            _postScaleWidthOffset += (overlayHeight.toFloat() * imageAspectRatio - overlayWidth) / 2
        }

//        androidTransformationMatrix.reset()
//        androidTransformationMatrix.setScale(_scaleFactor, _scaleFactor)
//        androidTransformationMatrix.postTranslate(-_postScaleWidthOffset, -_postScaleHeightOffset)

        transformationMatrix.reset()
        transformationMatrix.scale(_scaleFactor, _scaleFactor, 1f)
        transformationMatrix.translate(_postScaleWidthOffset, _postScaleHeightOffset, 0f)

        if (_isImageFlipped) {
            val flipMatrix = Matrix()
            flipMatrix.scale(-1f, 1f, 1f)
            flipMatrix.translate(overlayWidth / 2f, overlayHeight / 2f)

            transformationMatrix *= flipMatrix
//            androidTransformationMatrix.postScale(-1f, 1f, overlayWidth / 2f, overlayHeight / 2f)
        }

        needUpdateTransformation = false
    }

    /** Draws the overlay with its associated graphic objects.  */
    override fun drawOverlay(scope: DrawScope) {
        previousInvalidateToken.store(_invalidateToken.value)

        println("BaseGraphicOverlay::drawOverlay()")
        with(scope) {
            if (overlaySize.width != size.width || overlaySize.height != size.height) {
                println("BaseGraphicOverlay::drawOverlay() - new size received: $size")
                overlaySize = size
                needUpdateTransformation = true
            }

            updateTransformationIfNeeded()
            graphics.applyEach {
                draw()
            }
        }
    }

    override fun drawOverlay(scope: DrawScope, token: Long) {
        drawOverlay(scope)
    }

    fun invalidate() {
        println("BaseGraphicOverlay::invalidate()")
        if (previousInvalidateToken.load() == _invalidateToken.value) {
            _invalidateToken.value++
        }
    }
}