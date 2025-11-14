package dk.skancode.skanmate.util.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class SnackbarAdapter(
    private val snackbarChannel: Channel<SnackbarVisuals> = Channel(Channel.BUFFERED)
) {
    val snackbars: ReceiveChannel<SnackbarVisuals>
        get() = snackbarChannel

    suspend fun displaySnackbar(snackbar: String, config: Config = Config()) {
        println("SnackbarAdapter::displaySnackbar($snackbar, $config)")
        snackbarChannel.send(
            SkanMateSnackbarVisuals(
                message = snackbar,
                actionLabel = config.actionLabel,
                duration = config.duration,
                withDismissAction = config.withDismissAction,
                config = SkanMateSnackbarVisualsConfig(
                    description = config.description,
                    isError = config.isError,
                ),
            )
        )
    }

    data class Config(
        val isError: Boolean = false,
        val description: String? = null,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val withDismissAction: Boolean = false,
    )
}