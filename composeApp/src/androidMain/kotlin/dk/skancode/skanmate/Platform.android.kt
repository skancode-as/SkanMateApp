package dk.skancode.skanmate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.util.LocalCameraScanManager
import dk.skancode.barcodescannermodule.compose.LocalScannerModule
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream

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
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    AndroidCameraView(cameraUi = cameraUi)
}

@Composable
actual fun loadImageAsState(imagePath: String): State<Painter> {
    val fallbackBitmap = ImageBitmap(1, 1)
    val context = LocalContext.current

    val bitmap: Bitmap? = remember(context, imagePath) {
        val inputStream = context.contentResolver.openInputStream(imagePath.toUri())

        if (inputStream == null) {
            println("Could not find open input stream at imagePath: $imagePath")
            null
        }

        BitmapFactory.decodeStream(inputStream)
    }

    return rememberUpdatedState(BitmapPainter(bitmap?.asImageBitmap() ?: fallbackBitmap))
}