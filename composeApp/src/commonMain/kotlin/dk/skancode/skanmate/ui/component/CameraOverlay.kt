package dk.skancode.skanmate.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.CameraController
import dk.skancode.skanmate.util.animator
import dk.skancode.skanmate.util.toOneDecimalString
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.camera
import skanmate.composeapp.generated.resources.zap
import skanmate.composeapp.generated.resources.zap_off
import skanmate.composeapp.generated.resources.undo
import skanmate.composeapp.generated.resources.switch_camera

// TODO: Switch camera, maybe

@Composable
fun BoxScope.CameraOverlay(
    controller: CameraController,
    uiCameraController: UiCameraController = LocalUiCameraController.current,
) {
    val maxButtonSize = 64.dp
    val minButtonSize = 48.dp

    var isCapturing by remember { mutableStateOf(false) }
    var flashState by remember { mutableStateOf(controller.flashState) }
    val zoom by controller.zoomState

    ImageCapturingOverlay(
        painterIsLoading = isCapturing,
        onStopCapture = {
            uiCameraController.stopCamera()
        },
        onCaptureImage = {
            isCapturing = true
            controller.takePicture { res ->
                if (res.ok) {
                    uiCameraController.showPreview(res.data)
                    uiCameraController.stopCamera()
                }
                isCapturing = false
            }
        },
        flashState = flashState,
        onToggleFlash = { new ->
            if (controller.setFlashState(new)) {
                flashState = new
            }
        },
        onSwitchCamera = {},
        zoom = zoom,
        maxButtonSize = maxButtonSize,
        minButtonSize = minButtonSize,
    )
}

@OptIn(ExperimentalAnimationSpecApi::class)
@Composable
fun BoxScope.ImageCapturingOverlay(
    painterIsLoading: Boolean,
    onStopCapture: () -> Unit,
    onCaptureImage: () -> Unit,
    flashState: Boolean,
    onToggleFlash: (Boolean) -> Unit,
    zoom: Float,
    onSwitchCamera: () -> Unit,
    maxButtonSize: Dp = 64.dp,
    minButtonSize: Dp = 48.dp,
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(all = 16.dp)
            .fillMaxWidth(),
    ) {
        StopCaptureButton(
            modifier = Modifier
                .align(Alignment.CenterStart),
            onClick = onStopCapture,
            minSize = minButtonSize,
            maxSize = maxButtonSize,
        )

        ZoomBadge(
            modifier = Modifier.align(Alignment.Center),
            zoom = zoom,
        )

        ToggleFlashButton(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            value = flashState,
            onClick = onToggleFlash,
            minSize = minButtonSize,
            maxSize = maxButtonSize,
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(all = 16.dp)
            .fillMaxWidth(),
    ) {
        CaptureImageButton(
            modifier = Modifier
                .align(Alignment.Center),
            onClick = onCaptureImage,
            loading = painterIsLoading,
            minSize = minButtonSize,
            maxSize = maxButtonSize,
        )

        val animator = animator(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = ArcAnimationSpec(durationMillis = 400)
        )
        val rotation by animator.value
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            onClick = {
                animator.start()
                onSwitchCamera()
            },
            sizeValues = SizeValues(min = minButtonSize, max = maxButtonSize)
        ) {
            Icon(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .rotate(rotation.mod(360f)),
                imageVector = vectorResource(Res.drawable.switch_camera),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun ZoomBadge(
    modifier: Modifier = Modifier,
    zoom: Float,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val containerShape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .wrapContentSize()
            .clip(shape = containerShape)
            .shadow(elevation = 2.dp, shape = containerShape)
            .background(color = containerColor, shape = containerShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
            text = "${zoom.toOneDecimalString()}x",
            style = MaterialTheme.typography.labelLarge,
            color = contentColorFor(containerColor)
        )
    }
}

@Composable
fun StopCaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    retry: Boolean = false,
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
            imageVector = if (retry) vectorResource(Res.drawable.undo) else Icons.Default.Close,
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