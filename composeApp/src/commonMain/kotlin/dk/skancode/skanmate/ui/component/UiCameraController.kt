package dk.skancode.skanmate.ui.component

import dk.skancode.skanmate.ImageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi


fun interface ImageCaptureListener {
    fun handleAction(res: ImageCaptureAction)
}

/**
 * When action is Accept [data] is the accepted [ImageData] object
 *
 * When action is Discard [data] is the [ImageData] object to discard
 */
sealed class ImageCaptureAction(open val data: ImageData) {
    data class Accept(override val data: ImageData): ImageCaptureAction(data)
    data class Discard(override val data: ImageData): ImageCaptureAction(data)
}

@OptIn(ExperimentalAtomicApi::class)
class UiCameraController() {
    private var activeListener: AtomicReference<ImageCaptureListener?> = AtomicReference(null)
    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean>
        get() = _isStarted

    private val _preview = MutableStateFlow<ImageData?>(null)
    val preview: StateFlow<ImageData?>
        get() = _preview

    fun startCamera(listener: ImageCaptureListener) {
        println("Starting camera")
        _isStarted.compareAndSet(expect = false,  update = true)
        activeListener.store(listener)
    }
    fun stopCamera() {
        println("Stopping camera")
        _isStarted.compareAndSet(expect = true, update = false)
    }

    fun showPreview(p: ImageData?, listener: ImageCaptureListener? = null) {
        println("Showing preview")
        _preview.update { p }
        if (listener != null) {
            activeListener.store(listener)
        }
    }

    fun acceptPreview() {
        println("Accepting preview")
        val old = _preview.getAndUpdate { null }
        if (old != null) {
            activeListener.load()?.handleAction(res = ImageCaptureAction.Accept(data = old))
        }
        activeListener.store(null)
    }

    fun discardPreview() {
        println("Discarding preview")
        val old = _preview.getAndUpdate { null }
        val listener = activeListener.load()
        if (old != null) {
            listener?.handleAction(res = ImageCaptureAction.Discard(data = old))
        }
        startCamera(listener ?: throw IllegalStateException("discardPreview was called with no active listener"))
    }
}