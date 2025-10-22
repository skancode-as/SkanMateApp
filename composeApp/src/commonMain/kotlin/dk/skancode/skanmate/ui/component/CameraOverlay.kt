package dk.skancode.skanmate.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.CameraController
import dk.skancode.skanmate.ImageResourceState
import dk.skancode.skanmate.TakePictureResponse
import dk.skancode.skanmate.loadImage
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.camera
import skanmate.composeapp.generated.resources.zap
import skanmate.composeapp.generated.resources.zap_off

// TODO: Zoom on pinch and expand
// TODO: Switch camera, maybe

@Composable
fun BoxScope.CameraOverlay(
    controller: CameraController,
    uiCameraController: UiCameraController = LocalUiCameraController.current,
) {
    var imageResult by remember { mutableStateOf<TakePictureResponse?>(null) }
    val painterResource = loadImage(imageResult?.filePath)
    val painterIsLoading by painterResource.isLoading
    val painter by painterResource.state

    val maxButtonSize = 64.dp
    val minButtonSize = 48.dp

    if (imageResult != null && !painterIsLoading && painter is ImageResourceState.Image) {
        ImagePreviewOverlay(
            painter = (painter as ImageResourceState.Image<Painter>).data,
            onAcceptImage = { uiCameraController.onImageCapture(imageResult!!) },
            resetPreviewImageResult = { imageResult = null; painterResource.reset() },
            maxButtonSize = maxButtonSize,
            minButtonSize = minButtonSize,
        )
    } else {
        ImageCapturingOverlay(
            controller = controller,
            setImageResult = { imageResult = it },
            painterIsLoading = painterIsLoading,
            maxButtonSize = maxButtonSize,
            minButtonSize = minButtonSize,
        )
    }
}

@Composable
fun BoxScope.ImagePreviewOverlay(
    painter: Painter,
    resetPreviewImageResult: () -> Unit,
    onAcceptImage: () -> Unit,
    maxButtonSize: Dp = 64.dp,
    minButtonSize: Dp = 48.dp,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.Center)
    )

    StopCaptureButton(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 16.dp, start = 16.dp),
        onClick = {
            resetPreviewImageResult()
        },
        minSize = minButtonSize,
        maxSize = maxButtonSize,
    )

    AcceptImageButton(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp),
        onClick = {
            onAcceptImage()
        },
        minSize = minButtonSize,
        maxSize = maxButtonSize,
    )
}

@Composable
fun BoxScope.ImageCapturingOverlay(
    controller: CameraController,
    painterIsLoading: Boolean,
    setImageResult: (TakePictureResponse) -> Unit,
    uiCameraController: UiCameraController = LocalUiCameraController.current,
    maxButtonSize: Dp = 64.dp,
    minButtonSize: Dp = 48.dp,
) {
    val zoom by controller.zoomState

    Box(
        modifier = Modifier
            .wrapContentSize()
            .align(Alignment.TopCenter)
            .padding(top = 16.dp),
        propagateMinConstraints = true,
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(shape = MaterialTheme.shapes.extraLarge)
                .background(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.extraLarge)
        ) {
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = "${zoom}x",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }

    StopCaptureButton(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 16.dp, start = 16.dp),
        onClick = {
            uiCameraController.stopCamera()
        },
        minSize = minButtonSize,
        maxSize = maxButtonSize,
    )

    var flashState by remember { mutableStateOf(controller.flashState) }
    ToggleFlashButton(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp, end = 16.dp),
        value = flashState,
        onClick = { new ->
            if (controller.setFlashState(new)) {
                flashState = new
            }
        },
        minSize = minButtonSize,
        maxSize = maxButtonSize,
    )


    var isCapturing by remember { mutableStateOf(false) }
    CaptureImageButton(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp),
        onClick = {
            isCapturing = true
            controller.takePicture { res ->
                setImageResult(res)
                isCapturing = false
            }
        },
        loading = isCapturing || painterIsLoading,
        minSize = minButtonSize,
        maxSize = maxButtonSize,
    )
}

@Composable
fun StopCaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    minSize: Dp = 48.dp,
    maxSize: Dp = 64.dp
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        sizeValues = SizeValues(min = minSize, max = maxSize)
    ) {
        Icon(
            modifier = Modifier.minimumInteractiveComponentSize(),
            imageVector = Icons.Default.Close,
            contentDescription = null,
        )
    }
}

@Composable
fun ToggleFlashButton(
    modifier: Modifier = Modifier,
    value: Boolean,
    onClick: (Boolean) -> Unit,
    minSize: Dp = 48.dp,
    maxSize: Dp = 64.dp
) {
    IconButton(
        modifier = modifier,
        onClick = { onClick(!value) },
        sizeValues = SizeValues(min = minSize, max = maxSize)
    ) {
        Icon(
            modifier = Modifier.minimumInteractiveComponentSize(),
            imageVector = vectorResource(if(value) Res.drawable.zap else Res.drawable.zap_off),
            contentDescription = null,
        )
    }
}

@Composable
fun AcceptImageButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    minSize: Dp = 48.dp,
    maxSize: Dp = 64.dp
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        sizeValues = SizeValues(min = minSize, max = maxSize)
    ) {
        Icon(
            modifier = Modifier.minimumInteractiveComponentSize(),
            imageVector = Icons.Default.Check,
            contentDescription = null,
        )
    }
}

@Composable
fun CaptureImageButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    loading: Boolean = true,
    minSize: Dp = 48.dp,
    maxSize: Dp = 64.dp
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        sizeValues = SizeValues(min = minSize, max = maxSize),
        enabled = !loading,
    ) {
        AnimatedContent(loading) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier .minimumInteractiveComponentSize(),
                )
            } else {
                Icon(
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    imageVector = vectorResource(Res.drawable.camera),
                    contentDescription = null,
                )
            }
        }
    }
}