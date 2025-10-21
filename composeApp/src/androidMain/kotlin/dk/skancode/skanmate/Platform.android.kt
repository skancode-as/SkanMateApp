package dk.skancode.skanmate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import dk.skancode.skanmate.util.LocalCameraScanManager
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

@Composable
actual fun loadImage(imagePath: String?): ImageResource<Painter> {
    val resource = rememberImageResource()

    val context = LocalContext.current
    LaunchedEffect(context, imagePath, resource) {
        if (imagePath == null) return@LaunchedEffect
        resource.load()

        val inputStream = context.contentResolver.openInputStream(imagePath.toUri())

        if (inputStream == null) {
            println("Could not open input stream at imagePath: $imagePath")
            resource.error("Could not open input stream at imagePath: $imagePath")
            return@LaunchedEffect
        }

        val bitmap: Bitmap? =
            BitmapFactory.decodeStream(inputStream)?.rotate(90)

        if (bitmap != null) {
            resource.update(
                painter = BitmapPainter(bitmap.asImageBitmap())
            )
        }
    }

    return resource
}

fun Bitmap.rotate(degrees: Number): Bitmap {
    val matrix = Matrix().apply { preRotate(degrees.toFloat()) }
    val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    this.recycle()

    return rotatedBitmap
}