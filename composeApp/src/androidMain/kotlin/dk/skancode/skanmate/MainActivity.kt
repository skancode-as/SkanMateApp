package dk.skancode.skanmate

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import dk.skancode.skanmate.ui.component.barcode.CameraBarcodeScanner
import dk.skancode.skanmate.util.CameraScanManagerImpl
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dk.skancode.barcodescannermodule.ScannerActivity
import dk.skancode.barcodescannermodule.compose.ScannerModuleProvider
import dk.skancode.skanmate.camera.CameraPermissionAlert
import dk.skancode.skanmate.location.LocalLocationCollector
import dk.skancode.skanmate.location.LocationCollectorImpl
import dk.skancode.skanmate.location.LocationPermissionAlert
import dk.skancode.skanmate.location.LocationViewModel
import dk.skancode.skanmate.ui.theme.SkanMateTheme
import dk.skancode.skanmate.ui.viewmodel.CameraScanViewModel
import dk.skancode.skanmate.util.LocalAudioPlayer
import dk.skancode.skanmate.util.rememberMutableStateOf

private val cameraScanManager = CameraScanManagerImpl()

class MainActivity : ScannerActivity() {
    private lateinit var audioPlayer: AndroidAudioPlayer
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        audioPlayer = AndroidAudioPlayer(
            context = this,
        )

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            val factory = rememberPermissionsControllerFactory()
            val controller = remember(factory) { factory.createPermissionsController() }
            BindEffect(controller)

            val permissionsViewModel = viewModel { PermissionsViewModel(controller) }
            val cameraScanViewModel = viewModel { CameraScanViewModel(cameraScanManager) }
            val showCameraScanner by cameraScanViewModel.cameraPowerState.collectAsState()

            locationViewModel = viewModel {
                LocationViewModel(
                    locationPermission = permissionsViewModel.locationState,
                    fusedLocationClient = fusedLocationProviderClient
                )
            }

            val showLocationPermissionAlert = rememberMutableStateOf(false)

            val locationCollector = remember {
                LocationCollectorImpl(
                    locationFlow = locationViewModel.locationFlow,
                    startCollecting = {
                        if (permissionsViewModel.locationState != PermissionState.Granted) {
                            showLocationPermissionAlert.value = true
                        } else {
                            locationViewModel.startLocationRequests()
                        }
                    },
                    stopCollecting = {
                        if (permissionsViewModel.locationState == PermissionState.Granted) {
                            locationViewModel.stopLocationRequests()
                        }
                    },
                )
            }

            var showCameraAlert by remember { mutableStateOf(permissionsViewModel.cameraState != PermissionState.Granted) }

            ScannerModuleProvider {
                CompositionLocalProvider(
                    LocalCameraScanManager provides cameraScanManager,
                    LocalPermissionsViewModel provides permissionsViewModel,
                    LocalAudioPlayer provides audioPlayer,
                    LocalLocationCollector provides locationCollector,
                ) {
                    SkanMateTheme {
                        Scaffold { padding ->
                            App()
                            if (!scannerModule.scannerAvailable()) {
                                CameraBarcodeScanner(
                                    modifier = Modifier.padding(padding),
                                    showScanner = showCameraScanner,
                                    onSuccess = {
                                        cameraScanManager.send(it)
                                        cameraScanManager.stopScanning()
                                    },
                                    onFailed = {
                                        Log.e(
                                            "CameraBarcodeScanner",
                                            "Could not scan barcode",
                                            it
                                        )
                                        cameraScanManager.stopScanning()
                                    },
                                    onCancelled = { cameraScanManager.stopScanning() }
                                )
                                if (showCameraAlert && showCameraScanner) {
                                    CameraPermissionAlert(
                                        onDismissRequest = {
                                            showCameraAlert =
                                                false; cameraScanManager.stopScanning()
                                        },
                                    )
                                }
                            }


                            if (showLocationPermissionAlert.value) {
                                LocationPermissionAlert(
                                    onDismissRequest = { showLocationPermissionAlert.value = false },
                                    onGranted = {
                                        showLocationPermissionAlert.value = false
                                        locationViewModel.startLocationRequests()
                                    },
                                    closeable = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        locationViewModel.stopLocationRequests()
    }

    override fun onDestroy() {
        super.onDestroy()

        audioPlayer.release()
    }
}
