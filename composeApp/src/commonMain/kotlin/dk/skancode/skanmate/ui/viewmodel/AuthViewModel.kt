package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.data.service.AuthService
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.isValidEmail
import dk.skancode.skanmate.util.snackbar.UserMessageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.auth_screen_fill_credentials
import skanmate.composeapp.generated.resources.auth_screen_fill_email
import skanmate.composeapp.generated.resources.auth_screen_fill_pin
import skanmate.composeapp.generated.resources.auth_screen_invalid_email

class AuthViewModel(
    val authService: AuthService,
    val userMessageService: UserMessageService,
) : ViewModel() {
    private val _authedUser = MutableStateFlow<UserModel?>(null)
    val authedUser: StateFlow<UserModel?>
        get() = _authedUser
    private val _authedTenant = MutableStateFlow<TenantModel?>(null)
    val authedTenant: StateFlow<TenantModel?>
        get() = _authedTenant

    init {
        viewModelScope.launch {
            authService.userFlow.collect { userModel ->
                _authedUser.update { userModel }
            }
        }
        viewModelScope.launch {
            authService.tenantFlow.collect { tenantModel ->
                _authedTenant.update { tenantModel }
            }
        }
    }

    fun validateCredentials(email: String, pw: String): Boolean {
        return internalValidateCredentials(email, pw) == null
    }

    private fun internalValidateCredentials(email: String, pw: String): InternalStringResource? =
        when {
            email.isBlank() || pw.isBlank() -> {
                InternalStringResource(
                    Res.string.auth_screen_fill_credentials,
                )
            }

            email.isBlank() -> {
                InternalStringResource(
                    Res.string.auth_screen_fill_email,
                )
            }

            !email.isValidEmail() -> {
                InternalStringResource(
                    Res.string.auth_screen_invalid_email,
                )
            }

            pw.isBlank() -> {
                InternalStringResource(
                    Res.string.auth_screen_fill_pin,
                )
            }

            else -> null
        }


    fun signIn(email: String, pw: String, cb: (Boolean) -> Unit = {}) {
        val message = internalValidateCredentials(email, pw)

        when (message) {
            null -> viewModelScope.launch {
                val res = authService.signIn(email, pw)

                println("Sign in result: $res")
                _authedUser.update { res.user }
                _authedTenant.update { res.tenant }
                cb(res.user != null && res.tenant != null)
            }

            else -> {
                userMessageService.displayError(
                    message = message
                )
                cb(false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
        }
    }
}