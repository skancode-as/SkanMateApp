package dk.skancode.skanmate.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import dk.skancode.skanmate.util.keyboardVisibleAsState
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Composable
fun CameraBarcodeScanner(
    modifier: Modifier = Modifier,
    showScanner: Boolean,
    onSuccess: (Barcode) -> Unit = {},
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
        ScannerView(
            modifier = modifier,
            codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
        ) { result ->
            when (result) {
                BarcodeResult.OnCanceled -> onCancelled()
                is BarcodeResult.OnFailed -> onFailed(result.exception)
                is BarcodeResult.OnSuccess -> onSuccess(result.barcode)
            }
        }
    }
}