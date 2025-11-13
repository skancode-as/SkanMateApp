package dk.skancode.skanmate.util.snackbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

data class SkanMateSnackbarVisuals(
    override val message: String,
    override val withDismissAction: Boolean = false,
    override val actionLabel: String? = if (withDismissAction) "" else null,
    override val duration: SnackbarDuration = if (withDismissAction) SnackbarDuration.Indefinite else SnackbarDuration.Short,
    private val config: SkanMateSnackbarVisualsConfig = SkanMateSnackbarVisualsConfig()
): SnackbarVisuals {
    private val isError: Boolean by lazy { config.isError }
    val description: String? by lazy { config.description }
    val shape: Shape
        @Composable get() = SnackbarDefaults.shape
    val actionContentColor: Color
        @Composable get() = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
    val dismissActionContentColor: Color
        @Composable get() = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
    val containerColor: Color
        @Composable get() = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
    val contentColor: Color
        @Composable get() = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
}

data class SkanMateSnackbarVisualsConfig(
    val description: String? = null,
    val isError: Boolean = false,
)