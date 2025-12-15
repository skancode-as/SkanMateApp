package dk.skancode.skanmate

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.skancode.skanmate.data.service.AuthServiceImpl
import dk.skancode.skanmate.data.service.TableServiceImpl
import dk.skancode.skanmate.data.store.AuthStore
import dk.skancode.skanmate.data.store.LocalAuthStore
import dk.skancode.skanmate.data.store.LocalTableStore
import dk.skancode.skanmate.data.store.TableStore
import dk.skancode.skanmate.nav.AppNavHost
import dk.skancode.skanmate.ui.component.CameraOverlay
import dk.skancode.skanmate.ui.component.ImagePreview
import dk.skancode.skanmate.ui.component.LocalLabelTextStyle
import dk.skancode.skanmate.ui.component.LocalScanModule
import dk.skancode.skanmate.ui.component.LocalUiCameraController
import dk.skancode.skanmate.ui.component.UiCameraController
import dk.skancode.skanmate.ui.viewmodel.AuthViewModel
import dk.skancode.skanmate.ui.viewmodel.ConnectivityViewModel
import dk.skancode.skanmate.ui.viewmodel.InitializerViewModel
import dk.skancode.skanmate.ui.viewmodel.LocalConnectionState
import dk.skancode.skanmate.ui.viewmodel.SyncViewModel
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.clamp
import dk.skancode.skanmate.util.jsonSerializer
import dk.skancode.skanmate.util.snackbar.SnackbarAdapter
import dk.skancode.skanmate.util.snackbar.SnackbarHostProvider
import dk.skancode.skanmate.util.snackbar.SnackbarLayout
import dk.skancode.skanmate.util.snackbar.SnackbarManagerProvider
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import dk.skancode.skanmate.util.snackbar.snackbarHostStateProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

// TODO: Find less temporary way of initializing stores and services
// TODO: Haptic feedback (IOS)
// TODO: Store local data with tenant id, and filter out data not visible for current user
// TODO: Lock screen orientation

private const val BASE_URL =
    "https://skanmate-git-fix-api-tables-post-skan-code-team.vercel.app/api/v1"
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
    localAuthStore = LocalAuthStore(),
    externalScope = CoroutineScope(Dispatchers.IO)
)

private val tableService = TableServiceImpl(
    tableStore = tableStore,
    tokenFlow = authService.tokenFlow,
    tenantFlow = authService.tenantFlow,
    localTableStore = LocalTableStore(),
    externalScope = CoroutineScope(Dispatchers.IO)
)

private val snackbarAdapter: SnackbarAdapter = SnackbarAdapter()

private val ioScope = CoroutineScope(Dispatchers.IO)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    UserMessageServiceImpl.adapter = snackbarAdapter
    UserMessageServiceImpl.externalScope = ioScope

    val authViewModel = viewModel {
        AuthViewModel(authService, UserMessageServiceImpl)
    }
    val initializerViewModel = viewModel {
        InitializerViewModel(authService)
    }
    val tableViewModel = viewModel {
        TableViewModel(tableService, UserMessageServiceImpl)
    }
    val connectivityViewModel = viewModel {
        ConnectivityViewModel()
    }
    val syncViewModel = viewModel {
        SyncViewModel(tableService)
    }

    val scanModule = rememberScanModule()
    val uiCameraController = remember { UiCameraController() }
    val showCamera by uiCameraController.isStarted.collectAsState()
    val previewData by uiCameraController.preview.collectAsState()
    val preview: ImageResource<Painter> = loadImage(previewData?.path)

    CompositionLocalProvider(
        LocalScanModule provides scanModule,
        LocalUiCameraController provides uiCameraController,
        LocalLabelTextStyle provides MaterialTheme.typography.labelLarge,
        LocalConnectionState provides connectivityViewModel.connectionFlow.collectAsState()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            propagateMinConstraints = true,
        ) {
            val snackbarHostState = snackbarHostStateProvider(adapter = snackbarAdapter)
            SnackbarLayout(
                snackbar = { SnackbarHostProvider(adapter = snackbarAdapter, snackbarHostState) }
            ) {
                SnackbarManagerProvider(hostState = snackbarHostState) {
                    AppNavHost(
                        authViewModel = authViewModel,
                        initializerViewModel = initializerViewModel,
                        tableViewModel = tableViewModel,
                        syncViewModel = syncViewModel,
                    )
                }
            }
            if (showCamera) {
                var scale by remember { mutableFloatStateOf(1f)}
                val transformableState = rememberTransformableState { zoomChange, _, _ ->
                    scale = zoomChange
                }
                Scaffold { padding ->
                    CameraView(
                        modifier = Modifier
                            .padding(padding)
                            .transformable(state = transformableState),
                        cameraUi = { controller ->
                            CameraOverlay(controller = controller)

                            LaunchedEffect(scale) {
                                controller.zoom = (controller.zoom * scale)
                                    .clamp(minValue = controller.minZoom, maxValue = controller.maxZoom)
                            }
                        },
                    )
                }
            } else if (previewData != null) {
                val previewState by preview.state

                Scaffold { padding ->
                    when (previewState) {
                        is ImageResourceState.Image<Painter> -> {
                            ImagePreview(
                                modifier = Modifier.padding(padding),
                                preview = (previewState as ImageResourceState.Image<Painter>).data,
                                resetPreviewImageResult = { preview.reset() }
                            )
                        }

                        is ImageResourceState.Error -> {
                            Box(
                                modifier = Modifier.padding(padding).fillMaxSize(),
                                contentAlignment = Alignment.Center,
                                propagateMinConstraints = true
                            ) {
                                Text("Could not display image. ${(previewState as ImageResourceState.Error).error}")
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier.padding(padding).fillMaxSize(),
                                contentAlignment = Alignment.Center,
                                propagateMinConstraints = true
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.requiredSizeIn(minWidth = 64.dp, minHeight = 64.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}