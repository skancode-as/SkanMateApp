package dk.skancode.skanmate.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.icerock.moko.permissions.PermissionState
import dk.skancode.skanmate.LocalPermissionsViewModel
import dk.skancode.skanmate.R
import dk.skancode.skanmate.ui.component.ContentDialog
import dk.skancode.skanmate.ui.component.TextButton
import dk.skancode.skanmate.util.titleTextStyle
import dk.skancode.skanmate.util.unreachable

@Composable
fun CameraPermissionAlert(
    onDismissRequest: () -> Unit,
    closeable: Boolean = true,
) {
    val permissionsViewModel = LocalPermissionsViewModel.current ?: error("LocalPermissionsViewModel not provided in CameraView on Android")
    val cameraState = permissionsViewModel.cameraState

    when(cameraState) {
        PermissionState.Granted,
        PermissionState.NotDetermined -> return
        else -> {
            val onClick: () -> Unit = {
                when (cameraState) {
                    PermissionState.Denied,
                    PermissionState.NotGranted -> {
                        permissionsViewModel.provideOrRequestPermission()
                    }
                    PermissionState.DeniedAlways -> {
                        permissionsViewModel.openAppSettings()
                    }
                    else -> {}
                }
            }

            val padding = PaddingValues(16.dp)

            val description = @Composable {
                val text = when (cameraState) {
                    PermissionState.NotGranted -> {
                        stringResource(R.string.camera_permissions_not_granted)
                    }
                    PermissionState.Denied -> {
                        stringResource(R.string.camera_permissions_denied)
                    }
                    PermissionState.DeniedAlways -> {
                        stringResource(R.string.camera_permissions_denied)
                    }
                    else -> unreachable()
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            val content: @Composable ColumnScope.() -> Unit = {
                val buttonText = when (cameraState) {
                    PermissionState.Denied,
                    PermissionState.NotGranted -> {
                        stringResource(R.string.camera_permissions_give_permissions)
                    }
                    PermissionState.DeniedAlways -> {
                        stringResource(R.string.open_settings)
                    }
                    else -> unreachable()
                }

                Column(
                    modifier = Modifier.padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (cameraState == PermissionState.DeniedAlways) {
                        Text(
                            text = stringResource(R.string.camera_permissions_always_denied),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    Text(
                        text = stringResource(R.string.camera_permissions_for_what),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    TextButton(
                        onClick = onClick,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            ContentDialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                closable = closeable,
                contentPadding = padding,
                title = {
                    Text(
                        text = stringResource(R.string.camera_permissions_missing),
                        style = titleTextStyle(),
                    )
                },
                description = description,
                content = content,
            )
        }
    }
}