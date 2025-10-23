package dk.skancode.skanmate.ui.component

import dk.skancode.skanmate.TakePictureResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.atomics.ExperimentalAtomicApi


fun interface ImageCaptureListener {
    fun onImageCapture(res: TakePictureResponse)
}

@OptIn(ExperimentalAtomicApi::class)
class UiCameraController() {
    private val listeners = mutableSetOf<ImageCaptureListener>()
    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean>
        get() = _isStarted


    fun startCamera() {
        println("Starting camera")
        _isStarted.compareAndSet(expect = false,  update = true)
    }
    fun stopCamera() {
        println("Stopping camera")
        _isStarted.compareAndSet(expect = true, update = false)
    }

    fun registerListener(listener: ImageCaptureListener) {
        println("Registering listener")
        listeners.add(listener)
    }

    fun unregisterListener(listener: ImageCaptureListener) {
        println("Unregistering listener")
        listeners.remove(listener)
    }

    fun onImageCapture(res: TakePictureResponse) {
        println("UiCameraController::onImageCapture")
        listeners.forEach { it.onImageCapture(res = res) }
    }
}