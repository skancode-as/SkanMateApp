package dk.skancode.skanmate

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.launch

val LocalPermissionsViewModel: ProvidableCompositionLocal<PermissionsViewModel?> = compositionLocalOf { null }

class PermissionsViewModel(
    private val controller: PermissionsController,
) : ViewModel() {
    private var _cameraState by mutableStateOf(PermissionState.NotDetermined)

    val cameraState: PermissionState
        get() = _cameraState

    init {
        viewModelScope.launch {
            _cameraState = controller.getPermissionState(Permission.CAMERA)
        }
    }

    fun openAppSettings() {
        controller.openAppSettings()
    }

    fun provideOrRequestPermission() {
        viewModelScope.launch {
            try {
                controller.providePermission(Permission.CAMERA)
                _cameraState = PermissionState.Granted
            } catch (e: DeniedAlwaysException) {
                _cameraState = PermissionState.DeniedAlways
            } catch (e: DeniedException) {
                _cameraState = PermissionState.Denied
            } catch (e: RequestCanceledException) {
                e.printStackTrace()
            }
        }
    }
}