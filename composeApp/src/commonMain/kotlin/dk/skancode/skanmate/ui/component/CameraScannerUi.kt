package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ncgroup.kscan.ScannerController

@Composable
fun BoxScope.CameraScannerUi(
    onStopScan: () -> Unit,
    scannerController: ScannerController,
) {
    scannerController.torchEnabled
    
    CameraScannerOverlay(
        onStopCapture = onStopScan,
        flashState = scannerController.torchEnabled,
        onToggleFlash = { scannerController.setTorch(!scannerController.torchEnabled) },
        zoom = scannerController.zoomRatio,
    )
}

@Composable
fun BoxScope.CameraScannerOverlay(
    onStopCapture: () -> Unit,
    flashState: Boolean,
    onToggleFlash: (Boolean) -> Unit,
    zoom: Float,
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
}