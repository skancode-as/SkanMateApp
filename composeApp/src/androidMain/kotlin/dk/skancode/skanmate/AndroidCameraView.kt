package dk.skancode.skanmate

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
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import dev.icerock.moko.permissions.PermissionState
import dk.skancode.skanmate.util.unreachable
import dk.skancode.skanmate.util.clamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Composable
fun AndroidCameraView(
    modifier: Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    val permissionsViewModel = LocalPermissionsViewModel.current
    if (permissionsViewModel?.cameraState != PermissionState.Granted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundColor = MaterialTheme.colorScheme.background.value.toInt()

    val cameraExecutor = remember { Executors.newCachedThreadPool() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember {
        val p = PreviewView(
            context,
        )

        p.scaleType = PreviewView.ScaleType.FIT_CENTER
        p.setBackgroundColor(backgroundColor)

        p
    }

    val controller = remember(context) {
        AndroidCameraController(context, cameraProviderFuture, previewView, lifecycleOwner, cameraExecutor)
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        propagateMinConstraints = true,
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.align(Alignment.Center),
            update = {
                controller.updateView()
            },
            onRelease = {
                controller.onRelease()
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            cameraUi(controller)
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AndroidCameraController(
    val context: Context,
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    previewView: PreviewView,
    val lifecycleOwner: LifecycleOwner,
    val cameraExecutor: Executor,
): CameraController {
    private val preview: Preview = Preview.Builder().build()
    private val imageCapture: ImageCapture
    lateinit var camera: Camera
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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
    }

    fun updateView() {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .build()

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
        imageCapture.flashMode = if (v) FLASH_MODE_ON else FLASH_MODE_OFF
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