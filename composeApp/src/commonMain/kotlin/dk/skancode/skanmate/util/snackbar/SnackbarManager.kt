package dk.skancode.skanmate.util.snackbar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

interface SnackbarManager {
    val snackbarActive: Boolean
    val errorSnackbarActive: Boolean
}

private object SnackbarManagerImpl: SnackbarManager {
    lateinit var hostState: SnackbarHostState

    private val _visuals: SkanMateSnackbarVisuals?
        get() = hostState.currentSnackbarData?.visuals as? SkanMateSnackbarVisuals

    override val snackbarActive: Boolean
        get() = hostState.currentSnackbarData != null
    override val errorSnackbarActive: Boolean
        get() = _visuals?.isError ?: false
}

@Composable
fun SnackbarManagerProvider(
    hostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    SnackbarManagerImpl.hostState = hostState

    CompositionLocalProvider(
        value = LocalSnackbarManager provides SnackbarManagerImpl,
        content = content
    )
}

val LocalSnackbarManager: ProvidableCompositionLocal<SnackbarManager> = compositionLocalOf { SnackbarManagerImpl }