package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.component.FullWidthButton
import dk.skancode.skanmate.ui.component.InputField
import dk.skancode.skanmate.ui.component.TextTransformation
import dk.skancode.skanmate.ui.viewmodel.AuthViewModel
import dk.skancode.skanmate.util.HapticKind
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.LocalAudioPlayer
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.rememberHaptic
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.app_name
import skanmate.composeapp.generated.resources.auth_screen_email_label
import skanmate.composeapp.generated.resources.auth_screen_email_placeholder
import skanmate.composeapp.generated.resources.auth_screen_pin_label
import skanmate.composeapp.generated.resources.auth_screen_pin_placeholder
import skanmate.composeapp.generated.resources.auth_screen_sign_in
import skanmate.composeapp.generated.resources.auth_screen_sign_in_success
import skanmate.composeapp.generated.resources.scan_barcode

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    navigate: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(false) }
    val audioPlayer = LocalAudioPlayer.current
    val successHaptic = rememberHaptic(HapticKind.Success)
    val errorHaptic = rememberHaptic(HapticKind.Error)

    val submit = { email: String, pin: String ->
        isLoading = true
        viewModel.signIn(email, pin) { ok ->
            if (ok) {
                UserMessageServiceImpl.displayMessage(
                    message = InternalStringResource(
                        Res.string.auth_screen_sign_in_success,
                    )
                )
                audioPlayer.playSuccess()
                successHaptic.start()
                navigate()
            } else {
                audioPlayer.playError()
                errorHaptic.start()
            }

            isLoading = false
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                AppNameAndIcon()

                SignInCard(
                    validateCredentials = { email, pin ->
                        viewModel.validateCredentials(
                            email,
                            pin
                        )
                    },
                    submit = submit,
                    isLoading = isLoading,
                )
            }
        }
    }
}

@Composable
fun AppNameAndIcon() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(
                    shape = MaterialTheme.shapes.medium,
                )
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                )
                .requiredSize(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.scan_barcode),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(stringResource(Res.string.app_name), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SignInCard(
    validateCredentials: (email: String, pin: String) -> Boolean,
    submit: (email: String, pin: String) -> Unit,
    isLoading: Boolean,
) {
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier
            .padding(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    ) {
        val elementPadding = PaddingValues(16.dp)

        Column(
            modifier = Modifier.fillMaxWidth().padding(elementPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputField(
                modifier = Modifier.fillMaxWidth(),
                value = email,
                onValueChange = { email = it },
                label = {
                    Text(stringResource(Res.string.auth_screen_email_label))
                },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                placeholder = { Text(stringResource(Res.string.auth_screen_email_placeholder)) }
            )
            InputField(
                modifier = Modifier.fillMaxWidth(),
                value = pin,
                onValueChange = { pin = it },
                label = {
                    Text(stringResource(Res.string.auth_screen_pin_label))
                },
                placeholder = { Text(stringResource(Res.string.auth_screen_pin_placeholder)) },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                ),
                keyboardActions = KeyboardActions {
                    submit(email, pin)
                },
                textTransformation = TextTransformation.Password()
            )
            FullWidthButton(
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.darken(.1f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                onClick = { submit(email, pin) },
                enabled = validateCredentials(email, pin) && !isLoading,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(Res.string.auth_screen_sign_in),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                AnimatedVisibility(isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = LocalContentColor.current,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.darken(0.15f),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}