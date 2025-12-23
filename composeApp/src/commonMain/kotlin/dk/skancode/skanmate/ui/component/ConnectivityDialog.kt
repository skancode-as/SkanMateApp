package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dk.skancode.skanmate.data.service.ConnectivityMessage
import dk.skancode.skanmate.data.service.ConnectivityMessageResult
import dk.skancode.skanmate.util.titleTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.connectivity_dialog_btn_no
import skanmate.composeapp.generated.resources.connectivity_dialog_btn_yes
import skanmate.composeapp.generated.resources.connectivity_dialog_disable_offline_mode
import skanmate.composeapp.generated.resources.connectivity_dialog_enable_offline_mode
import skanmate.composeapp.generated.resources.connectivity_dialog_offline_mode_dialog_title
import skanmate.composeapp.generated.resources.connectivity_dialog_slow_internet_dialog_desc
import skanmate.composeapp.generated.resources.connectivity_dialog_slow_internet_dialog_title

@Composable
fun ConnectivityDialog(
    messageFlow: MutableStateFlow<ConnectivityMessage?>,
    sendMessageResult: suspend (ConnectivityMessageResult) -> Unit,
) {
    val message: ConnectivityMessage? by messageFlow.collectAsState()

    if (message != null) {
        MessageDialog(
            message = message!!,
            setMessage = { messageFlow.update { null } },
            sendMessageResult = sendMessageResult,
        )
    }
}

@Composable
fun MessageDialog(
    message: ConnectivityMessage,
    setMessage: (ConnectivityMessage?) -> Unit,
    sendMessageResult: suspend (ConnectivityMessageResult) -> Unit,
) {
    val contentSpacing = 16.dp
    val contentPadding = PaddingValues(contentSpacing)
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val hideDialog = {
        setMessage(null)
    }
    val onDismiss: () -> Unit = {
        scope.launch {
            sendMessageResult(ConnectivityMessageResult.Dismissed(message))
            hideDialog()
        }
    }
    val onAccept: () -> Unit = {
        scope.launch {
            sendMessageResult(ConnectivityMessageResult.Accepted(message))
            hideDialog()
        }
    }

    when (message) {
        is ConnectivityMessage.OfflineModeRequested -> {
            OfflineModeRequestDialog(
                onAccept = onAccept,
                onDismiss = onDismiss,
                enable = message.enabled,
                contentPadding = contentPadding,
                contentSpacing = contentSpacing,
            )
        }
        is ConnectivityMessage.RequestTimeout -> {
            RequestTimeoutDialog(
                onAccept = onAccept,
                onDismiss = onDismiss,
                contentPadding = contentPadding,
                contentSpacing = contentSpacing,
            )
        }
    }
}

@Composable
fun OfflineModeRequestDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    enable: Boolean,
    contentPadding: PaddingValues,
    contentSpacing: Dp,
) {
    ContentDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismiss,
        contentPadding = contentPadding,
        title = {
            Text(
                text = stringResource(Res.string.connectivity_dialog_offline_mode_dialog_title),
                style = titleTextStyle()
            )
        },
    ) {
        val text = when (enable) {
            true -> stringResource(Res.string.connectivity_dialog_enable_offline_mode)
            false -> stringResource(Res.string.connectivity_dialog_disable_offline_mode)
        }

        CompositionLocalProvider(LocalTextStyle provides LocalLabelTextStyle.current) {
            Column(
                modifier = Modifier.padding(paddingValues = contentPadding),
                verticalArrangement = Arrangement.spacedBy(space = contentSpacing)
            ) {
                Text(
                    text = text,
                )

                DialogButtons(
                    onAccept = onAccept,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
fun RequestTimeoutDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues,
    contentSpacing: Dp,
) {
    ContentDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismiss,
        contentPadding = contentPadding,
        title = {
            Text(
                text = stringResource(Res.string.connectivity_dialog_slow_internet_dialog_title),
                style = titleTextStyle()
            )
        },
    ) {
        val text =
            stringResource(Res.string.connectivity_dialog_slow_internet_dialog_desc)

        CompositionLocalProvider(LocalTextStyle provides LocalLabelTextStyle.current) {
            Column(
                modifier = Modifier.padding(paddingValues = contentPadding),
                verticalArrangement = Arrangement.spacedBy(space = contentSpacing)
            ) {
                Text(
                    text = text,
                )

                DialogButtons(
                    onAccept = onAccept,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
fun DialogButtons(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onDismiss,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            enabledWhenSnackbarActive = true,
        ) {
            Text(
                text = stringResource(Res.string.connectivity_dialog_btn_no),
            )
        }
        TextButton(
            onClick = onAccept,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            enabledWhenSnackbarActive = true,
        ) {
            Text(
                text = stringResource(Res.string.connectivity_dialog_btn_yes),
            )
        }
    }
}