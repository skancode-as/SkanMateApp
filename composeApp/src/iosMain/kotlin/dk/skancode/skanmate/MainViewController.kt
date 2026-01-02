package dk.skancode.skanmate

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.skancode.skanmate.location.LocalLocationCollector
import dk.skancode.skanmate.location.LocationAuthorizationAlert
import dk.skancode.skanmate.location.LocationCollectorImpl
import dk.skancode.skanmate.location.LocationViewModel
import dk.skancode.skanmate.ui.component.barcode.CameraBarcodeScanner
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dk.skancode.skanmate.ui.theme.SkanMateTheme
import dk.skancode.skanmate.ui.viewmodel.CameraScanViewModel
import dk.skancode.skanmate.util.CameraScanManagerImpl
import dk.skancode.skanmate.util.LocalAudioPlayer
import dk.skancode.skanmate.util.rememberMutableStateOf

private val cameraScanManager = CameraScanManagerImpl()

fun MainViewController() = ComposeUIViewController {
    val audioPlayer = IosAudioPlayer()

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    val cameraPowerViewModel = viewModel { CameraScanViewModel(cameraScanManager) }
    val locationViewModel = viewModel { LocationViewModel() }

    val showLocationPermissionAlert = rememberMutableStateOf(false)

    val locationCollector = remember(locationViewModel) {
        LocationCollectorImpl(
            locationFlow = locationViewModel.locationFlow,
            startCollecting = {
                if (!locationViewModel.isAuthorized) {
                    showLocationPermissionAlert.value = true
                } else {
                    locationViewModel.startLocationRequests()
                }
            },
            stopCollecting = {
                if (locationViewModel.isAuthorized) {
                    locationViewModel.stopLocationRequests()
                }
            },
        )
    }

    val showCameraScanner by cameraPowerViewModel.cameraPowerState.collectAsState()
    CompositionLocalProvider(
        LocalCameraScanManager provides cameraScanManager,
        LocalAudioPlayer provides audioPlayer,
        LocalLocationCollector provides locationCollector,
    ) {
        SkanMateTheme {
            Scaffold { padding ->
                App()

                CameraBarcodeScanner(
                    modifier = Modifier.padding(padding),
                    showScanner = showCameraScanner,
                    onSuccess = {
                        cameraScanManager.send(it)
                        cameraScanManager.stopScanning()
                    },
                    onFailed = {
                        println("CameraBarcodeScanner: Could not scan barcode due to exception. $it")
                        cameraScanManager.stopScanning()
                    },
                    onCancelled = { cameraScanManager.stopScanning() },
                )

                if (showLocationPermissionAlert.value) {
                    val authorizationRequestAllowed = locationViewModel.tryRequestAuthorization()

                    if (!authorizationRequestAllowed) {
                        LocationAuthorizationAlert(
                            locationState = locationViewModel.authorizationStatus,
                            onDismissRequest = { showLocationPermissionAlert.value = false },
                        )
                    }
                }
            }
        }
    }
}
