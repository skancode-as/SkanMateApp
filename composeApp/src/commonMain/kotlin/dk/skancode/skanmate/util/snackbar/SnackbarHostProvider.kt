package dk.skancode.skanmate.util.snackbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.component.IconButton

@Composable
fun SnackbarHostProvider(
    adapter: SnackbarAdapter,
    hostState: SnackbarHostState = snackbarHostStateProvider(adapter),
) {
    SnackbarHost(hostState) { data ->
        val visuals: SkanMateSnackbarVisuals? = data.visuals as? SkanMateSnackbarVisuals
        if (visuals == null) {
            Snackbar(data)
        } else {
            val actionLabel = visuals.actionLabel
            val actionComposable: (@Composable () -> Unit)? =
                if (actionLabel != null) {
                    @Composable {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                            onClick = { data.performAction() },
                            content = { Text(actionLabel) }
                        )
                    }
                } else {
                    null
                }
            val dismissActionComposable: (@Composable () -> Unit)? =
                if (visuals.withDismissAction) {
                    @Composable {
                        IconButton(
                            onClick = { data.dismiss() },
                            content = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss snackbar icon",
                                )
                            }
                        )
                    }
                } else {
                    null
                }

            Snackbar(
                modifier = Modifier.padding(12.dp),
                action = actionComposable,
                dismissAction = dismissActionComposable,
                actionOnNewLine = false,
                containerColor = visuals.containerColor,
                contentColor = visuals.contentColor,
                shape = visuals.shape,
                actionContentColor = visuals.actionContentColor,
                dismissActionContentColor = visuals.dismissActionContentColor,
            ) {
                val description = visuals.description
                if (description == null) {
                    Text(visuals.message)
                } else {
                    Column {
                        Text(visuals.message, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
