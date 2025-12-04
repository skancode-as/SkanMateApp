package dk.skancode.skanmate.camera

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.icerock.moko.permissions.PermissionState
import dk.skancode.skanmate.CameraController
import dk.skancode.skanmate.LocalPermissionsViewModel
import java.util.concurrent.Executors

@Composable
fun AndroidCameraView(
    modifier: Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    val permissionsViewModel = LocalPermissionsViewModel.current
    if (permissionsViewModel?.cameraState != PermissionState.Granted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundColor = MaterialTheme.colorScheme.background.value.toInt()

    val cameraExecutor = remember { Executors.newCachedThreadPool() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember {
        val p = PreviewView(
            context,
        )

        p.scaleType = PreviewView.ScaleType.FIT_CENTER
        p.setBackgroundColor(backgroundColor)

        p
    }

    val controller = remember(context) {
        AndroidCameraController(
            context,
            cameraProviderFuture,
            previewView,
            lifecycleOwner,
            cameraExecutor,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        propagateMinConstraints = true,
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.align(Alignment.Center),
            update = {
                controller.updateView()
            },
            onRelease = {
                controller.onRelease()
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            cameraUi(controller)
        }
    }
}
