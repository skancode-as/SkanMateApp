package dk.skancode.skanmate

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.impl.utils.Exif
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dk.skancode.barcodescannermodule.compose.LocalScannerModule
import androidx.core.net.toUri

@Composable
actual fun rememberScanModule(): ScanModule {
    val localModule = LocalScannerModule.current
    val localCameraScanManager = LocalCameraScanManager.current

    return remember(localModule) {
        AndroidScanModuleImpl(localModule, localCameraScanManager)
    }
}

actual val platformSettingsFactory: Settings.Factory = SkanMateApplication.settingsFactory

@Composable
actual fun CameraView(
    modifier: Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    AndroidCameraView(modifier = modifier, cameraUi = cameraUi)
}

@SuppressLint("RestrictedApi")
@Composable
actual fun loadImage(imagePath: String?): ImageResource<Painter> {
    val resource = rememberImageResource()

    val context = LocalContext.current
    LaunchedEffect(context, imagePath, resource) {
        if (imagePath == null) return@LaunchedEffect
        resource.load()

        var inputStream = context.contentResolver.openInputStream(imagePath.toUri())

        if (inputStream == null) {
            println("Could not open input stream at imagePath: $imagePath")
            resource.error("Could not open input stream at imagePath: $imagePath")
            return@LaunchedEffect
        }

        val rotation = inputStream.use { inputStream ->
            val exif = Exif.createFromInputStream(inputStream)
            exif.rotation
        }
        inputStream = context.contentResolver.openInputStream(imagePath.toUri())

        if (inputStream == null) {
            println("Could not open input stream at imagePath: $imagePath")
            resource.error("Could not open input stream at imagePath: $imagePath")
            return@LaunchedEffect
        }
        inputStream.use { inputStream ->
            val bitmap: Bitmap? =
                BitmapFactory.decodeStream(inputStream)?.rotate(rotation)

            if (bitmap != null) {
                resource.update(
                    painter = BitmapPainter(bitmap.asImageBitmap())
                )
            }
        }
    }

    return resource
}

fun Bitmap.rotate(degrees: Number): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    this.recycle()

    return rotatedBitmap
}

actual suspend fun deleteFile(path: String) {
    SkanMateApplication.deleteLocalFile(path)
}