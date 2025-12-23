package dk.skancode.skanmate.camera

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import dk.skancode.skanmate.CameraController
import dk.skancode.skanmate.ImageData
import dk.skancode.skanmate.R
import dk.skancode.skanmate.TakePictureResponse
import dk.skancode.skanmate.util.clamp
import dk.skancode.skanmate.util.unreachable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class AndroidCameraController(
    val context: Context,
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    previewView: PreviewView,
    val lifecycleOwner: LifecycleOwner,
    val cameraExecutor: Executor,
    //val barcodeSettings: AndroidBarcodeSettings = AndroidBarcodeSettings(),
): CameraController {
    private val preview: Preview = Preview.Builder().build()
    private var imageCapture: ImageCapture
    lateinit var camera: Camera
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    //private lateinit var imageAnalysis: ImageAnalysis

    private val _flashState = AtomicBoolean(false)
    override val flashState: Boolean
        get() = _flashState.load()

    override val minZoom: Float
        get() = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
    override val maxZoom: Float
        get() = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f

    private val _zoom = MutableStateFlow(1f)

    override var zoom: Float
        get() = _zoom.value
        set(value) = updateZoom(value)
    override val zoomState: State<Float>
        @Composable get() = _zoom.collectAsState()

    private val _canSwitchCamera: MutableState<Boolean> = mutableStateOf(false)

    override val canSwitchCamera: State<Boolean>
        get() = _canSwitchCamera
    override val canTakePicture: Boolean
        get() = this::camera.isInitialized

    init {
        preview.surfaceProvider = previewView.surfaceProvider
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelectorSet = cameraProvider.availableCameraInfos
                .map { info ->  info.lensFacing }
                .filter { it == CameraSelector.LENS_FACING_BACK || it == CameraSelector.LENS_FACING_FRONT }
                .toMutableSet()
            _canSwitchCamera.value = cameraSelectorSet.size > 1
        }, ContextCompat.getMainExecutor(context))

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(preview.targetRotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(80)
            .build()

//        if (barcodeSettings.enabled) {
//            imageAnalysis = ImageAnalysis.Builder()
//                .setResolutionSelector(
//                    ResolutionSelector.Builder()
//                        .setResolutionStrategy(
//                            ResolutionStrategy(
//                                imageAnalysisSize,
//                                ResolutionStrategy.FALLBACK_RULE_NONE,
//                            )
//                        )
//                        .build()
//                )
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//        }
    }

//    private fun imageAnalyzer(barcodeGraphicOverlay: BarcodeGraphicOverlay): AndroidBarcodeAnalyzer {
//        val options = BarcodeScannerOptions.Builder()
//            .setBarcodeFormats(
//                AndroidBarcodeAnalyzer.getMLKitBarcodeFormats(barcodeSettings.codeTypes)
//            )
//            .build()
//
//        val barcodeScanner = BarcodeScanning.getClient(options)
//
//        return AndroidBarcodeAnalyzer(
//            scanner = barcodeScanner,
//            onSuccess = {
//                barcodeSettings.onSuccess?.invoke(it)
//            },
//            onFailed = barcodeSettings.onFailed ?: {},
//            onCanceled = barcodeSettings.onCanceled ?: {},
//            graphicOverlay = barcodeGraphicOverlay,
//        )
//    }

    fun updateView() {
        println("AndroidCameraController::updateView()")
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()

        val useCaseGroupBuilder = UseCaseGroup.Builder()
            .addUseCase(preview)

//        if (::imageAnalysis.isInitialized) {
//            val barcodeGraphicOverlay = BarcodeGraphicOverlay(context = context, attrs = null)
//            previewView.addView(barcodeGraphicOverlay)
//
//            imageAnalysis.setAnalyzer(
//                ContextCompat.getMainExecutor(context),
//                imageAnalyzer(barcodeGraphicOverlay)
//            )
//
//            useCaseGroupBuilder.addUseCase(imageAnalysis)
//        } else {
            useCaseGroupBuilder.addUseCase(imageCapture)
        //}

        val useCaseGroup = useCaseGroupBuilder.build()

        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
    }

    fun onRelease() {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }

    private fun updateZoom(newZoom: Float) {
        val actualNewZoom = newZoom.clamp(minZoom, maxZoom)
        _zoom.update { actualNewZoom }

        camera.cameraControl.setZoomRatio(actualNewZoom)
    }

    override fun setFlashState(v: Boolean): Boolean {
        imageCapture.flashMode = if (v) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        _flashState.store(v)

        return true
    }

    override fun takePicture(cb: (TakePictureResponse) -> Unit) {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val appName = context.resources.getString(R.string.app_name)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$appName")
            }
        }

        val outputFileOptions = ImageCapture
            .OutputFileOptions
            .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()
        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            CaptureListener(
                cb = cb,
                imageName = name,
                contentResolver = context.contentResolver,
            ),
        )
    }

    override fun switchCamera() {
        cameraSelector = when (cameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> unreachable("[AndroidCameraController] : CameraSelector has been set to a camera other than front or back")
        }

        updateView()
    }

    private data class CaptureListener(
        val cb: (TakePictureResponse) -> Unit,
        val imageName: String,
        val contentResolver: ContentResolver,
    ): ImageCapture.OnImageSavedCallback {
        @SuppressLint("RestrictedApi")
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val uri = outputFileResults.savedUri
            val imagePath = outputFileResults.savedUri?.toString()
            println("Image saved on location: $imagePath")

            val bytes: ByteArray? = if (uri != null) {
                contentResolver.openInputStream(uri)?.readBytes()
            } else null

            cb(
                TakePictureResponse(
                    ok = bytes != null,
                    data = ImageData(
                        path = imagePath,
                        name = imageName,
                        data = bytes,
                    ),
                    error = if (bytes != null) null
                    else "Could not read data from image location $imagePath",
                )
            )
        }

        override fun onError(exception: ImageCaptureException) {
            println("Image capture failed with exception: $exception")
            cb(
                TakePictureResponse(
                    ok = false,
                    data = null,
                    error = exception.message ?: "Image capture failed with exception: $exception"
                )
            )
        }
    }
}