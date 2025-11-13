package dk.skancode.skanmate.util.snackbar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun snackbarHostStateProvider(
    adapter: SnackbarAdapter,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(adapter) {
        for (snackbar in adapter.snackbars) {
            snackbarHostState.showSnackbar(snackbar)
        }
    }

    return snackbarHostState
}