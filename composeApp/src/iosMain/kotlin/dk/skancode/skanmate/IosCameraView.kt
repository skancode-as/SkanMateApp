package dk.skancode.skanmate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.UIKitViewController
import dk.skancode.skanmate.ui.component.LocalUiCameraController
import dk.skancode.skanmate.util.clamp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isFlashActive
import platform.AVFoundation.position
import platform.AVFoundation.torchMode
import platform.AVFoundation.videoZoomFactor
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalForeignApi::class)
@Composable
fun IosCameraView(
    modifier: Modifier = Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
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
    val uiCameraController = LocalUiCameraController.current

    val cameraUiKitViewController = remember {
        CameraUiKitViewController(
            device = device,
            onError = {
                uiCameraController.stopCamera()
            }
        )
    }

    val controller = remember {
        IosCameraController(
            device = device,
            viewController = cameraUiKitViewController
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        propagateMinConstraints = true,
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

        Box(
            modifier = modifier.fillMaxWidth().wrapContentHeight(),
            propagateMinConstraints = true,
        ) {
            UIKitViewController(
                factory = { cameraUiKitViewController },
                modifier = Modifier
                    .size(
                        width = with(density) { (windowSize.width - (rightInset + leftInset)).toDp() },
                        height = with(density) { ((windowSize.width - (rightInset + leftInset)) * 5f / 4f).toDp() }
                    )
                    .fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            cameraUi(controller)
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
class IosCameraController(
    private val device: AVCaptureDevice,
    private val viewController: CameraUiKitViewController,
) : CameraController {
    private val _flashState = AtomicBoolean(device.isFlashActive())

    override val flashState: Boolean
        get() = _flashState.load()
    private val _canSwitchCamera = mutableStateOf(true)
    override val canSwitchCamera: State<Boolean>
        get() = _canSwitchCamera
    override val canTakePicture: Boolean
        get() = viewController.canTakePicture

    override val minZoom: Float = 1f
    override val maxZoom: Float
        get() = device.activeFormat.videoMaxZoomFactor.toFloat()

    private var _zoom = MutableStateFlow(1.0f)

    override var zoom: Float
        get() = _zoom.value
        set(value) = updateZoom(value)

    override val zoomState: State<Float>
        @Composable get() = _zoom.collectAsState()

    @OptIn(ExperimentalForeignApi::class)
    private fun updateZoom(newZoom: Float) {
        if (device.lockForConfiguration(null)) {
            val actualNewZoom = newZoom.clamp(minZoom, maxZoom).toDouble()

            _zoom.update { actualNewZoom.toFloat() }
            device.videoZoomFactor = actualNewZoom

            device.unlockForConfiguration()
        } else {
            throw Exception("Could not lock device for configuration")
        }
    }

    override fun takePicture(cb: (TakePictureResponse) -> Unit) {
        println("IosCameraController::takePicture($cb)")
        val flashMode = when {
            flashState -> AVCaptureFlashModeOn
            else -> AVCaptureFlashModeOff
        }

        viewController.takePicture(flashMode, cb)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun switchCamera() {
        viewController.switchCamera()
    }

    override fun setFlashState(v: Boolean): Boolean {
        val res = updateTorch(v)
        if (res) {
            _flashState.store(v)
        }
        return res
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun updateTorch(enabled: Boolean): Boolean {
        if (device.hasTorch) {
            var locked = false
            try {
                locked = device.lockForConfiguration(null)
                if (locked) {
                    device.torchMode =
                        if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                    _flashState.store(enabled)
                }
            } catch (_: Throwable) {
                return false
            } finally {
                if (locked) {
                    device.unlockForConfiguration()
                }
            }
            return true
        }
        return false
    }
}