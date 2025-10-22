package dk.skancode.skanmate.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.ui.viewmodel.AuthViewModel

val LocalAuthUser: ProvidableCompositionLocal<UserModel> = compositionLocalOf { UserModel.empty() }
val LocalAuthTenant: ProvidableCompositionLocal<TenantModel> = compositionLocalOf { TenantModel.empty() }

@Composable
fun AuthenticationRequired(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    onUnauthorized: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val user by authViewModel.authedUser.collectAsState()
    val tenant by authViewModel.authedTenant.collectAsState()

    LaunchedEffect(user, tenant) {
        if (user == null || tenant == null) {
            onUnauthorized()
        }
    }

    AnimatedContent(
        targetState = user,
        modifier = modifier,
    ) { targetUser ->
        if (targetUser != null && tenant != null) {
            CompositionLocalProvider(
                LocalAuthUser provides targetUser,
                LocalAuthTenant provides tenant!!
            ) {
                content()
            }
        } else {
            SignedOut()
        }
    }
}

@Composable
private fun SignedOut() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Text("You have been signed out.")
        }
    }
}