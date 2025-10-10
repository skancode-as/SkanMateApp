package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.component.Button
import dk.skancode.skanmate.ui.component.InputField
import dk.skancode.skanmate.ui.viewmodel.AuthViewModel
import dk.skancode.skanmate.util.darken
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.scan_barcode

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    navigate: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val submit = {
        isLoading = true
        viewModel.signIn(email, pin) { ok ->
            if (ok) {
                navigate()
            }

            isLoading = false
        }
    }

    Scaffold { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .padding(8.dp)
                .fillMaxSize()
                .imePadding(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
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
                        Text("SkanMate", fontWeight = FontWeight.SemiBold)
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .padding(16.dp)
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
                                    Text("Email")
                                },
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                ),
                                placeholder = { Text("Your email...") }
                            )
                            InputField(
                                modifier = Modifier.fillMaxWidth(),
                                value = pin,
                                onValueChange = { pin = it },
                                label = {
                                    Text("Pin")
                                },
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done,
                                    autoCorrectEnabled = false,
                                    capitalization = KeyboardCapitalization.None,
                                ),
                                keyboardActions = KeyboardActions {
                                    submit()
                                },
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Button(
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.darken(.1f),
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                onClick = submit,
                                enabled = !isLoading,
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                                    text = "Sign in",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                AnimatedVisibility(isLoading) {
                                    Box(
                                        modifier = Modifier.size(20.dp),
                                        contentAlignment = Alignment.Center,
                                        propagateMinConstraints = true,
                                    ) {
                                        CircularProgressIndicator(
                                            color = LocalContentColor.current,
                                            trackColor = MaterialTheme.colorScheme.primaryContainer.darken(0.15f),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}