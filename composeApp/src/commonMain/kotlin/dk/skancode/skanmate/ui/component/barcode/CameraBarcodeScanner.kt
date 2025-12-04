package dk.skancode.skanmate.ui.component.barcode

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import dk.skancode.skanmate.ScannerController
import dk.skancode.skanmate.SkanMateScannerView
import dk.skancode.skanmate.util.HapticKind
import dk.skancode.skanmate.util.clamp
import dk.skancode.skanmate.util.keyboardVisibleAsState
import dk.skancode.skanmate.util.rememberHaptic

@Composable
fun CameraBarcodeScanner(
    modifier: Modifier = Modifier,
    showScanner: Boolean,
    onSuccess: (List<BarcodeData>) -> Unit = {},
    onFailed: (Exception) -> Unit = {},
    onCancelled: () -> Unit = {},
) {
    if (showScanner) {
        val isKeyboardVisible by keyboardVisibleAsState()
        if (isKeyboardVisible) {
            val localSoftwareKeyboardController = LocalSoftwareKeyboardController.current
            LaunchedEffect(localSoftwareKeyboardController) {
                localSoftwareKeyboardController?.hide()
            }
        }
        val successHaptic = rememberHaptic(kind = HapticKind.Success)

        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current

        val (leftInset, rightInset) = remember(density, layoutDirection) {
            val insets = WindowInsets()

            val leftToRight = insets.getLeft(density, layoutDirection) to insets.getRight(
                density,
                layoutDirection
            )

            leftToRight
        }
        val windowSize = LocalWindowInfo.current.containerSize

        val scannerController = remember { ScannerController() }
        val transformableState = rememberTransformableState { zoomChange, _, _ ->
            val zoom = (scannerController.zoomRatio * zoomChange)

            scannerController.setZoom(zoom)
        }
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background),
            propagateMinConstraints = true,
        ) {
            SkanMateScannerView(
                modifier = Modifier
                    .size(
                        width = with(density) { (windowSize.width - (rightInset + leftInset)).toDp() },
                        height = with(density) { ((windowSize.width - (rightInset + leftInset)) * 5f / 4f).toDp() }
                    )
                    .align(Alignment.Center)
                    .transformable(transformableState),
                scannerController = scannerController,
                codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
            ) { result ->
                when (result) {
                    BarcodeResult.OnCanceled -> onCancelled()
                    is BarcodeResult.OnFailed -> onFailed(result.exception)
                    is BarcodeResult.OnSuccess -> {
                        successHaptic.start()
                        onSuccess(result.barcodes)
                    }
                }
            }
            Box(
                modifier = modifier
                    .fillMaxSize(),
            ) {
                CameraScannerUi(
                    onStopScan = onCancelled,
                    scannerController = scannerController,
                )
            }
        }
    }
}