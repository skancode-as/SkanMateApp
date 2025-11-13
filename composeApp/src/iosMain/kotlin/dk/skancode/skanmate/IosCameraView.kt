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
import androidx.compose.ui.viewinterop.UIKitView
import dk.skancode.skanmate.util.clamp
import dk.skancode.skanmate.util.currentDateTimeUTC
import dk.skancode.skanmate.util.format
import dk.skancode.skanmate.util.unreachable
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureDeferredPhotoProxy
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessNotReadyMomentarily
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForCapture
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForProcessing
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessReady
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessSessionNotRunning
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureResolvedPhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecJPEG
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.isFlashActive
import platform.AVFoundation.position
import platform.AVFoundation.videoZoomFactor
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMTime
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.darwin.NSObject
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

    val output = remember { AVCapturePhotoOutput() }

    val session = remember {
        AVCaptureSession().also { session ->
            session.sessionPreset = AVCaptureSessionPresetPhoto
            val input: AVCaptureDeviceInput = deviceInputWithDevice(device = device, error = null)!!
            if (session.canAddInput(input)) {
                session.addInput(input)
            } else {
                error("Could not add camera input")
            }
            if (session.canAddOutput(output)) {
                session.addOutput(output)
            } else {
                error("Could not add camera input")
            }
        }
    }

    val controller = remember {
        IosCameraController(
            device = device,
            output = output,
            session = session,
        )
    }

    val cameraPreviewLayer: AVCaptureVideoPreviewLayer =
        remember { AVCaptureVideoPreviewLayer(session = session) }

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
            UIKitView(
                factory = {
                    val container = object : UIView(frame = CGRectZero.readValue()) {
                        override fun layoutSubviews() {
                            CATransaction.begin()
                            CATransaction.setValue(true, kCATransactionDisableActions)
                            layer.setFrame(frame)
                            cameraPreviewLayer.setFrame(frame)
                            CATransaction.commit()
                        }
                    }
                    container.layer.addSublayer(cameraPreviewLayer)
                    cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                    controller.onCreate()
                    container
                },
                update = { view ->
                    println("UIKitView::update($view)")
                    controller.onUpdate()
                },
                onRelease = {
                    controller.onRelease()
                },
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
    private val output: AVCapturePhotoOutput,
    private val session: AVCaptureSession,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : CameraController {
    private val _flashState = AtomicBoolean(device.isFlashActive())

    override val flashState: Boolean
        get() = _flashState.load()
    private val _canSwitchCamera = mutableStateOf(true)
    override val canSwitchCamera: State<Boolean>
        get() = _canSwitchCamera
    override val canTakePicture: Boolean
        get() = output.captureReadiness == AVCapturePhotoOutputCaptureReadinessReady

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
    fun onCreate() {
        if (!session.running) {
            externalScope.launch {
                session.startRunning()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun onUpdate() {
        println("IosCameraController::onUpdate()")
    }

    fun onRelease() {
        if (session.running) {
            session.stopRunning()
        }
    }

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
        val settings =
            AVCapturePhotoSettings
                .photoSettingsWithFormat(format = mapOf(AVVideoCodecKey to AVVideoCodecJPEG))

        settings.setFlashMode(
            if (flashState) AVCaptureFlashModeOn
            else AVCaptureFlashModeOff
        )
        val captureReadiness = when (output.captureReadiness) {
            AVCapturePhotoOutputCaptureReadinessReady -> "Ready"
            AVCapturePhotoOutputCaptureReadinessNotReadyMomentarily -> "Not ready momentarily"
            AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForCapture -> "Not ready - Waiting for capture"
            AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForProcessing -> "Not ready - Waiting for processing"
            AVCapturePhotoOutputCaptureReadinessSessionNotRunning -> "Not ready - Session not running"
            else -> "Unknown capture readiness"
        }
        println("IosCameraController::takePicture() - settings = $settings, captureReadiness: $captureReadiness")
        output.capturePhotoWithSettings(settings, OutputCapturer(cb))
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun switchCamera() {
        externalScope.launch {
            session.beginConfiguration()
            val currentInput = session.inputs.firstOrNull() as AVCaptureDeviceInput?
            if (currentInput != null) {
                session.removeInput(currentInput)
                val newDevicePosition = when (currentInput.device.position) {
                    AVCaptureDevicePositionBack -> AVCaptureDevicePositionFront
                    AVCaptureDevicePositionFront -> AVCaptureDevicePositionBack
                    else -> unreachable("[IosCameraController] : Current input devices position is neither Front nor Back")
                }

                @Suppress("UNCHECKED_CAST")
                val devices =
                    AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo) as List<AVCaptureDevice>
                val newDevice = devices.firstOrNull { it.position == newDevicePosition }
                if (newDevice != null) {
                    val newInput = AVCaptureDeviceInput(newDevice, null)
                    session.addInput(newInput)
                } else {
                    val devicePositionString = when (newDevicePosition) {
                        AVCaptureDevicePositionBack -> "Back"
                        AVCaptureDevicePositionFront -> "Front"
                        else -> unreachable()
                    }
                    println("Could not find video device with position: $devicePositionString")
                }
            } else {
                println("Session does not have a input connected")
            }

            session.commitConfiguration()
        }
    }

    override fun setFlashState(v: Boolean): Boolean {
        _flashState.store(v)
        return true
    }

    private class OutputCapturer(val cb: (TakePictureResponse) -> Unit) : NSObject(),
        AVCapturePhotoCaptureDelegateProtocol {

        init {
            println("IosCameraController::OutputCapturer::init($cb)")
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun captureOutput(
            output: AVCapturePhotoOutput,
            willBeginCaptureForResolvedSettings: AVCaptureResolvedPhotoSettings
        ) {
            println("IosCameraController::OutputCapturer::captureOutput::willBeginCapture($output, $willBeginCaptureForResolvedSettings)")
            println("IosCameraController::OutputCapturer::captureOutput::willBeginCapture() - photoDimensions: ${willBeginCaptureForResolvedSettings.photoDimensions}")
        }

        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishCapturingDeferredPhotoProxy: AVCaptureDeferredPhotoProxy?,
            error: NSError?
        ) {
            cb(TakePictureResponse(
                ok = false,
                data = null,
                error = "Received DeferredPhotoProxy"
            ))
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingLivePhotoToMovieFileAtURL: NSURL,
            duration: CValue<CMTime>,
            photoDisplayTime: CValue<CMTime>,
            resolvedSettings: AVCaptureResolvedPhotoSettings,
            error: NSError?
        ) {
            cb(TakePictureResponse(
                ok = false,
                data = null,
                error = "Received LivePhotoToMovieFileURL"
            ))
        }

        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishCaptureForResolvedSettings: AVCaptureResolvedPhotoSettings,
            error: NSError?
        ) {
            cb(TakePictureResponse(
                ok = false,
                data = null,
                error = "Received didFinishCaptureForResolvedSettings"
            ))
        }

        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingPhoto: AVCapturePhoto,
            error: NSError?
        ) {
            println("IosCameraController::OutputCapturer::captureOutput()")
            if (error != null) {
                cb(
                    TakePictureResponse(
                        ok = false,
                        data = null,
                        error = error.description ?: "Unknown error occurred"
                    )
                )
            }

            val photoData = didFinishProcessingPhoto.fileDataRepresentation()
            val cbRes = if (photoData != null) {
                val dirPath = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                )[0] as String
                val fileName = "${currentDateTimeUTC().format("yyyy-MM-dd-HH-mm-ss")}.jpg"
                val filePath = "$dirPath/$fileName"

                if (photoData.writeToFile(path = filePath, atomically = false)) {
                    println("Image saved at:\n$filePath")
                    TakePictureResponse(
                        ok = true,
                        data = ImageData(
                            path = fileName,
                            name = fileName,
                            data = photoData.toByteArray(),
                        ),
                        error = null,
                    )
                } else {
                    TakePictureResponse(
                        ok = false,
                        data = null,
                        error = "Could not write photo to file"
                    )
                }
            } else {
                TakePictureResponse(
                    ok = false,
                    data = null,
                    error = "No photo data was received"
                )
            }
            cb(cbRes)
        }
    }
}