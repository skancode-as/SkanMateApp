package dk.skancode.skanmate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.skancode.skanmate.data.service.AuthServiceImpl
import dk.skancode.skanmate.data.service.TableServiceImpl
import dk.skancode.skanmate.data.store.AuthStore
import dk.skancode.skanmate.data.store.TableStore
import dk.skancode.skanmate.nav.AppNavHost
import dk.skancode.skanmate.ui.component.CameraOverlay
import dk.skancode.skanmate.ui.component.LocalUiCameraController
import dk.skancode.skanmate.ui.component.UiCameraController
import dk.skancode.skanmate.ui.viewmodel.AuthViewModel
import dk.skancode.skanmate.ui.viewmodel.InitializerViewModel
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.jsonSerializer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val BASE_URL =
    "https://skanmate-git-feat-insert-data-endpoint-skan-code-team.vercel.app/api/v1"
private val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(jsonSerializer)
    }
}

private val authStore = AuthStore(
    baseUrl = BASE_URL,
    client = httpClient,
)
private val tableStore = TableStore(
    baseUrl = BASE_URL,
    client = httpClient,
)
private val authService = AuthServiceImpl(
    authStore = authStore,
    externalScope = CoroutineScope(Dispatchers.IO)
)

private val tableService = TableServiceImpl(
    tableStore = tableStore,
    tokenFlow = authService.tokenFlow,
    externalScope = CoroutineScope(Dispatchers.IO)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val authViewModel = viewModel {
        AuthViewModel(authService)
    }
    val initializerViewModel = viewModel {
        InitializerViewModel(authService)
    }
    val tableViewModel = viewModel {
        TableViewModel(tableService)
    }

    MaterialTheme {
        val scanModule = rememberScanModule()
        val uiCameraController = remember { UiCameraController() }
        val showCamera by uiCameraController.isStarted.collectAsState()

        CompositionLocalProvider(
            LocalScanModule provides scanModule,
            LocalUiCameraController provides uiCameraController,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                propagateMinConstraints = true,
            ) {
                AppNavHost(
                    authViewModel = authViewModel,
                    initializerViewModel = initializerViewModel,
                    tableViewModel = tableViewModel,
                )
                if (showCamera) {
                    Scaffold { padding ->
                        CameraView(
                            modifier = Modifier.padding(padding),
                            cameraUi = { CameraOverlay(controller = it) }
                        )
                    }
                }
            }
        }
    }
}

val LocalScanModule: ProvidableCompositionLocal<ScanModule> = compositionLocalOf {
    object : ScanModule {
        override fun isHardwareScanner(): Boolean {
            TODO("Not yet implemented")
        }

        override fun registerListener(handler: ScanEventHandler) {
            TODO("Not yet implemented")
        }

        override fun unregisterListener(handler: ScanEventHandler) {
            TODO("Not yet implemented")
        }

        override fun enableScan() {
            TODO("Not yet implemented")
        }

        override fun disableScan() {
            TODO("Not yet implemented")
        }
    }
}