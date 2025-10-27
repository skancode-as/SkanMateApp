package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ImagePreview(
    modifier: Modifier = Modifier,
    preview: Painter,
    uiCameraController: UiCameraController = LocalUiCameraController.current,
) {
    val minButtonSize = 48.dp
    val maxButtonSize = 64.dp
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Image(
            painter = preview,
            modifier = Modifier.align(Alignment.Center),
            contentDescription = null,
        )

        ImagePreviewOverlay(
            painter = preview,
            onAcceptImage = { uiCameraController.acceptPreview() },
            resetPreviewImageResult = { uiCameraController.discardPreview() },
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
        retry = true,
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
