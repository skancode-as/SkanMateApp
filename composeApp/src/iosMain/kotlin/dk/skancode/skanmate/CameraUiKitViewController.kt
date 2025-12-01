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
import org.ncgroup.kscan.BarcodeFormat
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureFlashMode
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
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
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.AVVideoCodecJPEG
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.position
import platform.CoreGraphics.CGFloat
import platform.Foundation.NSDictionary
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
import platform.darwin.dispatch_get_main_queue
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.ios_camera_not_available

class CameraUiKitViewController(
    private val device: AVCaptureDevice,
    private val onError: () -> Unit,
    private val codeTypes: List<BarcodeFormat> = emptyList(),
    private val onBarcodes: ((List<BarcodeResult>) -> Unit)? = null,
    private val scanningEnabled: Boolean = codeTypes.isNotEmpty() && onBarcodes != null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): UIViewController(null, null), AVCapturePhotoCaptureDelegateProtocol,
    AVCaptureMetadataOutputObjectsDelegateProtocol {

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
            println("CameraUiKitViewController::setupCaptureSession() - Could not add video input to session")
            return false
        }
        session.addInput(videoInput)

        if (!session.canAddOutput(photoOutput)) {
            println("CameraUiKitViewController::setupCaptureSession() - Could not add photo output to session")
            return false
        }
        session.addOutput(photoOutput)

        if (scanningEnabled) {
            if(!setupScanOutput()) {
                return false
            }
        }

        setupPreviewLayer()
        startSession()
        return true
    }

    private fun setupScanOutput(): Boolean {
        val metadataOutput = AVCaptureMetadataOutput()
        if (!session.canAddOutput(metadataOutput)) {
            println("CameraUiKitViewController::setupScanOutput() - Could not add metadata output to session")
            return false
        }
        session.addOutput(metadataOutput)

        return setupMetadataOutput(metadataOutput)
    }

    private fun setupMetadataOutput(output: AVCaptureMetadataOutput): Boolean {
        output.setMetadataObjectsDelegate(this, dispatch_get_main_queue())

        val supportedTypes = getMetadataObjectTypes()
        if (supportedTypes.isEmpty()) {
            println("CameraUiKitViewController::setupMetadataOutput() - No supported output types")
            return false
        }
        output.metadataObjectTypes = supportedTypes

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

    data class BarcodeResult(val value: String, val type: String, val rawBytes: ByteArray, val corners: List<BarcodeCorner>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as BarcodeResult

            if (value != other.value) return false
            if (type != other.type) return false
            if (!rawBytes.contentEquals(other.rawBytes)) return false
            if (corners != other.corners) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + rawBytes.contentHashCode()
            result = 31 * result + corners.hashCode()
            return result
        }
    }

    data class BarcodeCorner(val x: Double, val y: Double)

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        if (::previewLayer.isInitialized && scanningEnabled && onBarcodes != null) {
            didOutputMetadataObjects
                .filterIsInstance<AVMetadataMachineReadableCodeObject>()
                .mapNotNull { metadata ->
                    previewLayer.transformedMetadataObjectForMetadataObject(metadata) as? AVMetadataMachineReadableCodeObject
                }
                .filter { barcode ->
                    isRequestedFormat(barcode.type)
                }
                .mapNotNull { barcode ->
                    if (barcode.stringValue != null) {
                        BarcodeResult(
                            value = barcode.stringValue!!,
                            type = barcode.type.toFormat().toString(),
                            rawBytes = barcode.stringValue!!.encodeToByteArray(),
                            corners = barcode.corners
                                .filterIsInstance<NSDictionary>()
                                .map { pointDict ->

                                    BarcodeCorner(
                                        x = pointDict.objectForKey("X") as? CGFloat ?: 0.0,
                                        y = pointDict.objectForKey("Y") as? CGFloat ?: 0.0,
                                    )
                                },
                        )
                    } else {
                        null
                    }
                }.apply {
                    onBarcodes(this)
                }
        }
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

    private fun getMetadataObjectTypes(): List<AVMetadataObjectType> {
        if (codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return ALL_SUPPORTED_AV_TYPES
        }

        return codeTypes.mapNotNull { appFormat ->
            APP_TO_AV_FORMAT_MAP[appFormat]
        }
    }

    private fun isRequestedFormat(type: AVMetadataObjectType): Boolean {
        if (codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return AV_TO_APP_FORMAT_MAP.containsKey(type)
        }

        val appFormat = AV_TO_APP_FORMAT_MAP[type] ?: return false

        return codeTypes.contains(appFormat)
    }

    private fun AVMetadataObjectType.toFormat(): BarcodeFormat {
        return AV_TO_APP_FORMAT_MAP[this] ?: BarcodeFormat.TYPE_UNKNOWN
    }

    private val AV_TO_APP_FORMAT_MAP: Map<AVMetadataObjectType, BarcodeFormat> =
        mapOf(
            AVMetadataObjectTypeQRCode to BarcodeFormat.FORMAT_QR_CODE,
            AVMetadataObjectTypeEAN13Code to BarcodeFormat.FORMAT_EAN_13,
            AVMetadataObjectTypeEAN8Code to BarcodeFormat.FORMAT_EAN_8,
            AVMetadataObjectTypeCode128Code to BarcodeFormat.FORMAT_CODE_128,
            AVMetadataObjectTypeCode39Code to BarcodeFormat.FORMAT_CODE_39,
            AVMetadataObjectTypeCode93Code to BarcodeFormat.FORMAT_CODE_93,
            AVMetadataObjectTypeUPCECode to BarcodeFormat.FORMAT_UPC_E,
            AVMetadataObjectTypePDF417Code to BarcodeFormat.FORMAT_PDF417,
            AVMetadataObjectTypeAztecCode to BarcodeFormat.FORMAT_AZTEC,
            AVMetadataObjectTypeDataMatrixCode to BarcodeFormat.FORMAT_DATA_MATRIX,
        )

    private val APP_TO_AV_FORMAT_MAP: Map<BarcodeFormat, AVMetadataObjectType> =
        AV_TO_APP_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }

    val ALL_SUPPORTED_AV_TYPES: List<AVMetadataObjectType> = AV_TO_APP_FORMAT_MAP.keys.toList()

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