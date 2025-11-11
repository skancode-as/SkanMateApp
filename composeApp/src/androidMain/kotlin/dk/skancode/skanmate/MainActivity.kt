package dk.skancode.skanmate

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.skancode.skanmate.ui.component.CameraBarcodeScanner
import dk.skancode.skanmate.util.CameraScanManagerImpl
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dk.skancode.barcodescannermodule.ScannerActivity
import dk.skancode.barcodescannermodule.compose.ScannerModuleProvider
import dk.skancode.skanmate.ui.viewmodel.CameraScanViewModel

private val cameraScanManager = CameraScanManagerImpl()

class MainActivity : ScannerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val factory = rememberPermissionsControllerFactory()
            val controller = remember(factory) { factory.createPermissionsController() }
            BindEffect(controller)

            val permissionsViewModel = viewModel { PermissionsViewModel(controller) }
            val cameraScanViewModel = viewModel { CameraScanViewModel(cameraScanManager) }
            val showCameraScanner by cameraScanViewModel.cameraPowerState.collectAsState()

            ScannerModuleProvider {
                CompositionLocalProvider(
                    LocalCameraScanManager provides cameraScanManager,
                    LocalPermissionsViewModel provides permissionsViewModel
                ) {
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
                                Log.e(
                                    "CameraBarcodeScanner",
                                    "Could not scan barcode",
                                    it
                                )
                                cameraScanManager.stopScanning()
                            },
                            onCancelled = { cameraScanManager.stopScanning() }
                        )
                        CameraPermissionAlert()
                    }
                }
            }
        }
    }
}
