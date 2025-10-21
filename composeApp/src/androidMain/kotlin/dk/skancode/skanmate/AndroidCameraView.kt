package dk.skancode.skanmate

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun AndroidCameraView(
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor = remember { Executors.newCachedThreadPool() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    val controller = remember(context) { AndroidCameraController(context, cameraExecutor) }

    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true,
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                preview.surfaceProvider = previewView.surfaceProvider
                val cameraSelector =
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                val imageCapture = ImageCapture.Builder()
                    .setTargetRotation(preview.targetRotation)
                    .build()

                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture, preview)

                controller.imageCapture = imageCapture
            }
        )
        cameraUi(controller)
    }
}

class AndroidCameraController(
    val context: Context,
    val cameraExecutor: Executor,
): CameraController {
    lateinit var imageCapture: ImageCapture

    override fun takePicture(cb: (TakePictureResponse) -> Unit) {
        if (!this::imageCapture.isInitialized) {
            cb(
                TakePictureResponse(
                    ok = false,
                    filePath = null,
                    filename = null,
                    fileData = null,
                    error = "Controller was not fully initialized"
                )
            )
            return
        }

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

    private data class CaptureListener(
        val cb: (TakePictureResponse) -> Unit,
        val imageName: String,
        val contentResolver: ContentResolver,
    ): ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val imagePath = outputFileResults.savedUri?.toString()
            println("Image saved on location: $imagePath")

            val bytes: ByteArray? = if (outputFileResults.savedUri != null) {
                val inputStream = contentResolver.openInputStream(outputFileResults.savedUri!!)
                if (inputStream == null) {
                    println("Could not open input stream at imagePath: $imagePath")
                    null
                } else {
                    inputStream.readBytes()
                }
            } else null

            cb(
                TakePictureResponse(
                    ok = true,
                    filePath = imagePath,
                    filename = imageName,
                    fileData = bytes,
                    error = null,
                )
            )
        }

        override fun onError(exception: ImageCaptureException) {
            println("Image capture failed with exception: $exception")
            cb(
                TakePictureResponse(
                    ok = false,
                    filePath = null,
                    filename = null,
                    fileData = null,
                    error = exception.message ?: "Image capture failed with exception: $exception"
                )
            )
        }
    }
}