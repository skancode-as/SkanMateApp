package dk.skancode.skanmate

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.skancode.skanmate.ui.component.CameraBarcodeScanner
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dk.skancode.skanmate.ui.viewmodel.CameraScanViewModel
import dk.skancode.skanmate.util.CameraScanManagerImpl

private val cameraScanManager = CameraScanManagerImpl()

fun MainViewController() = ComposeUIViewController {
    val cameraPowerViewModel = viewModel { CameraScanViewModel(cameraScanManager) }

    val showCameraScanner by cameraPowerViewModel.cameraPowerState.collectAsState()
    CompositionLocalProvider(LocalCameraScanManager provides cameraScanManager) {
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
                onCancelled = { cameraScanManager.stopScanning() })
        }
    }
}
