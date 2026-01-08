package dk.skancode.skanmate.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dk.skancode.skanmate.ui.component.ContentDialog
import dk.skancode.skanmate.ui.component.TextButton
import dk.skancode.skanmate.util.titleTextStyle
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.ios_location_authorization_for_what
import skanmate.composeapp.generated.resources.ios_location_authorization_missing
import skanmate.composeapp.generated.resources.open_settings

@Composable
fun LocationAuthorizationAlert(
    locationState: LocationAuthorizationStatus,
    onDismissRequest: () -> Unit,
    closeable: Boolean = true,
) {
    if (!locationState.isAuthorized) {
        val padding = PaddingValues(16.dp)

        ContentDialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            closable = closeable,
            contentPadding = padding,
            title = {
                Text(
                    text = stringResource(Res.string.ios_location_authorization_missing),
                    style = titleTextStyle(),
                )
            }
        ) {
            Column(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.ios_location_authorization_for_what),
                    style = MaterialTheme.typography.bodyLarge,
                )

                TextButton(
                    onClick = {
                        UIApplication.sharedApplication.openURL(
                            NSURL(string = UIApplicationOpenSettingsURLString),
                            emptyMap<Any?, Any?>()
                        ) { _ -> }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.open_settings),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}