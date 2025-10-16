package dk.skancode.skanmate

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.util.LocalCameraScanManager
import dk.skancode.barcodescannermodule.compose.LocalScannerModule

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