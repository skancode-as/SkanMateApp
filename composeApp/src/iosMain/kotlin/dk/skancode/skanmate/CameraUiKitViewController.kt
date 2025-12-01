package dk.skancode.skanmate

import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.currentDateTimeUTC
import dk.skancode.skanmate.util.format
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import dk.skancode.skanmate.util.unreachable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureFlashMode
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessNotReadyMomentarily
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForCapture
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForProcessing
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessReady
import platform.AVFoundation.AVCapturePhotoOutputCaptureReadinessSessionNotRunning
import platform.AVFoundation.AVCapturePhotoQualityPrioritizationSpeed
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecJPEG
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.position
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIViewController
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.ios_camera_not_available

class CameraUiKitViewController(
    private val device: AVCaptureDevice,
    private val onError: () -> Unit,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): UIViewController(null, null), AVCapturePhotoCaptureDelegateProtocol {
    private var cb: (TakePictureResponse) -> Unit = {}
    private lateinit var session: AVCaptureSession
    private lateinit var previewLayer: AVCaptureVideoPreviewLayer
    private lateinit var videoInput: AVCaptureDeviceInput
    private val photoOutput: AVCapturePhotoOutput = AVCapturePhotoOutput()

    val canTakePicture: Boolean
        get() = photoOutput.captureReadiness == AVCapturePhotoOutputCaptureReadinessReady

    override fun viewDidLoad() {
        super.viewDidLoad()
        if (!setupCamera()) {
            UserMessageServiceImpl.displayError(
                message = InternalStringResource(resource = Res.string.ios_camera_not_available),
            )
            onError()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCamera(): Boolean {
        session = AVCaptureSession()
        try {
            videoInput = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as AVCaptureDeviceInput
        } catch (e: Exception) {
            println("CameraUiKitViewController::setupCamera() - Could not create video input: $e")
            return false
        }

        return setupCaptureSession()
    }

    private fun setupCaptureSession(): Boolean {
        session.sessionPreset = AVCaptureSessionPresetPhoto

        if (!session.canAddInput(videoInput)) {
            println("CameraUiKitViewController::setupCaptureCamera() - Could not add video input to session")
            return false
        }
        session.addInput(videoInput)

        if (!session.canAddOutput(photoOutput)) {
            println("CameraUiKitViewController::setupCaptureCamera() - Could not add photo output to session")
            return false
        }
        session.addOutput(photoOutput)

        setupPreviewLayer()
        startSession()
        return true
    }

    fun takePicture(flashMode: AVCaptureFlashMode, cb: (TakePictureResponse) -> Unit) {
        println("CameraUiKitViewController::takePicture($flashMode, $cb)")
        this.cb = cb
        val settings =
            AVCapturePhotoSettings
                .photoSettingsWithFormat(format = mapOf(AVVideoCodecKey to AVVideoCodecJPEG))

        settings.setFlashMode(flashMode)
        settings.photoQualityPrioritization = AVCapturePhotoQualityPrioritizationSpeed
        val captureReadiness = when (photoOutput.captureReadiness) {
            AVCapturePhotoOutputCaptureReadinessReady -> "Ready"
            AVCapturePhotoOutputCaptureReadinessNotReadyMomentarily -> "Not ready momentarily"
            AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForCapture -> "Not ready - Waiting for capture"
            AVCapturePhotoOutputCaptureReadinessNotReadyWaitingForProcessing -> "Not ready - Waiting for processing"
            AVCapturePhotoOutputCaptureReadinessSessionNotRunning -> "Not ready - Session not running"
            else -> "Unknown capture readiness"
        }
        println("CameraUiKitViewController::takePicture() - settings = $settings, captureReadiness: $captureReadiness")
        photoOutput.capturePhotoWithSettings(settings, this)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun switchCamera() {
        externalScope.launch {
            session.beginConfiguration()
            val currentInput = session.inputs.firstOrNull() as AVCaptureDeviceInput?
            if (currentInput != null) {
                session.removeInput(currentInput)
                val newDevicePosition = when (currentInput.device.position) {
                    AVCaptureDevicePositionBack -> AVCaptureDevicePositionFront
                    AVCaptureDevicePositionFront -> AVCaptureDevicePositionBack
                    else -> unreachable("[CameraUiKitViewController] : Current input devices position is neither Front nor Back")
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

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        if (!session.isRunning()) {
            startSession()
        }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        if (session.isRunning()) {
            stopSession()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if (::previewLayer.isInitialized) {
            previewLayer.frame = view.layer.bounds
            updatePreviewOrientation()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupPreviewLayer() {
        previewLayer = AVCaptureVideoPreviewLayer.layerWithSession(session)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(previewLayer)
        updatePreviewOrientation()
    }

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        println("CameraUiKitViewController::captureOutput()")
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

    private fun updatePreviewOrientation() {
        if (!::previewLayer.isInitialized) return

        val connection = previewLayer.connection ?: return

        val uiOrientation: UIInterfaceOrientation = UIApplication.sharedApplication().statusBarOrientation

        val videoOrientation: AVCaptureVideoOrientation =
            when (uiOrientation) {
                UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
                UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
                UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
                else -> AVCaptureVideoOrientationPortrait
            }

        connection.videoOrientation = videoOrientation
    }

    private fun startSession() = externalScope.launch {
        session.startRunning()
    }
    private fun stopSession() = externalScope.launch {
        session.stopRunning()
    }

    fun dispose() {
        // Best-effort cleanup to avoid retaining camera resources
        runCatching {
            if (::session.isInitialized) {
                if (session.isRunning()) session.stopRunning()
                // Remove inputs/outputs to break potential retain cycles
                (session.outputs as? List<AVCaptureOutput>)?.forEach { output ->
                    runCatching { session.removeOutput(output) }
                }
                (session.inputs as? List<AVCaptureDeviceInput>)?.forEach { input ->
                    runCatching { session.removeInput(input) }
                }
            }
        }
        runCatching {
            if (::previewLayer.isInitialized) {
                previewLayer.removeFromSuperlayer()
            }
        }
    }
}