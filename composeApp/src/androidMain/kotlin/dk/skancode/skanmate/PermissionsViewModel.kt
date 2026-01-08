package dk.skancode.skanmate

import android.Manifest
import android.content.Context
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
import dev.icerock.moko.permissions.PermissionDelegate
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.launch

val LocalPermissionsViewModel: ProvidableCompositionLocal<PermissionsViewModel?> =
    compositionLocalOf { null }

object AllLocationDelegate : PermissionDelegate {
    override fun getPermissionStateOverride(applicationContext: Context): PermissionState? = null

    override fun getPlatformPermission(): List<String> =
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

}

object AllLocationPermission : Permission {
    override val delegate: PermissionDelegate = AllLocationDelegate
}

val Permission.Companion.ALL_LOCATION: Permission get() = AllLocationPermission

class PermissionsViewModel(
    private val controller: PermissionsController,
) : ViewModel() {
    private var _cameraState by mutableStateOf(PermissionState.NotDetermined)
    private var _locationState by mutableStateOf(PermissionState.NotDetermined)

    val cameraState: PermissionState
        get() = _cameraState
    val locationState: PermissionState
        get() = _locationState

    init {
        viewModelScope.launch {
            _cameraState = controller.getPermissionState(Permission.CAMERA)
        }
        viewModelScope.launch {
            _locationState = controller.getPermissionState(Permission.ALL_LOCATION)
        }
    }

    fun openAppSettings() {
        controller.openAppSettings()
    }

    fun provideOrRequestCameraPermission() {
        viewModelScope.launch {
            _cameraState = provideOrRequestPermission(Permission.CAMERA)
        }
    }

    fun provideOrRequestLocationPermission(onGranted: () -> Unit) {
        viewModelScope.launch {
            val result = provideOrRequestPermission(Permission.ALL_LOCATION)
            _locationState = result
            if (result == PermissionState.Granted) {
                onGranted()
            }
        }
    }

    private suspend fun provideOrRequestPermission(permission: Permission): PermissionState {
        return try {
            controller.providePermission(permission)
            PermissionState.Granted
        } catch (_: DeniedAlwaysException) {
            PermissionState.DeniedAlways
        } catch (_: DeniedException) {
            PermissionState.Denied
        } catch (e: RequestCanceledException) {
            e.printStackTrace()
            PermissionState.NotGranted
        }
    }
}