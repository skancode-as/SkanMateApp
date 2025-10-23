package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
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