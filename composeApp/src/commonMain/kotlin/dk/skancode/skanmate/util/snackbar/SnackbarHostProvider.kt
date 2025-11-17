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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ScanModule
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.IconButton
import dk.skancode.skanmate.ui.component.LocalScanModule
import dk.skancode.skanmate.ui.component.TextButton

@Composable
fun SnackbarHostProvider(
    adapter: SnackbarAdapter,
    hostState: SnackbarHostState = snackbarHostStateProvider(adapter),
    scanModule: ScanModule = LocalScanModule.current,
) {
    if (scanModule.isHardwareScanner()) {
        val currentSnackbarData = hostState.currentSnackbarData
        LaunchedEffect(currentSnackbarData) {
            if (currentSnackbarData != null) {
                val visuals = currentSnackbarData.visuals as? SkanMateSnackbarVisuals
                if (visuals != null && visuals.isError) {
                    scanModule.disableScan()
                }
            } else {
                scanModule.enableScan()
            }
        }
    }

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
                            enabledWhenSnackbarActive = true,
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
                            },
                            elevation = CustomButtonElevation.None,
                            enabledWhenSnackbarActive = true,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = visuals.containerColor,
                                contentColor = visuals.dismissActionContentColor,
                            )
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
