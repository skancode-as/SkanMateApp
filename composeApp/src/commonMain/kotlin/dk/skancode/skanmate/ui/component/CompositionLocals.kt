package dk.skancode.skanmate.ui.component

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import dk.skancode.skanmate.ScanEventHandler
import dk.skancode.skanmate.ScanModule
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.util.CameraScanManager
import dk.skancode.skanmate.util.CameraScanManagerImpl

val LocalScanModule: ProvidableCompositionLocal<ScanModule> = compositionLocalOf {
    object : ScanModule {
        override fun isHardwareScanner(): Boolean = false
        override fun registerListener(handler: ScanEventHandler) {}
        override fun unregisterListener(handler: ScanEventHandler) {}
        override fun enableScan() {}
        override fun disableScan() {}
    }
}

val LocalAuthUser: ProvidableCompositionLocal<UserModel> = compositionLocalOf { UserModel.empty() }
val LocalAuthTenant: ProvidableCompositionLocal<TenantModel> =
    compositionLocalOf { TenantModel.empty() }

val LocalUiCameraController: ProvidableCompositionLocal<UiCameraController> = compositionLocalOf {
    UiCameraController()
}

val LocalCameraScanManager: ProvidableCompositionLocal<CameraScanManager> =
    compositionLocalOf { CameraScanManagerImpl() }

val LocalLabelTextStyle: ProvidableCompositionLocal<TextStyle> = compositionLocalOf { TextStyle() }