package dk.skancode.skanmate.util.snackbar

import androidx.compose.material3.SnackbarVisuals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class SnackbarAdapter(
    private val snackbarChannel: Channel<SnackbarVisuals> = Channel(Channel.BUFFERED)
) {
    val snackbars: ReceiveChannel<SnackbarVisuals>
        get() = snackbarChannel

}