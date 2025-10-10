package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.data.service.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    val authService: AuthService,
): ViewModel() {
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

    fun signIn(email: String, pw: String, cb: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = authService.signIn(email, pw)

            println("Sign in result: $res")
            _authedUser.update { res.user }
            _authedTenant.update { res.tenant }
            cb(res.user != null && res.tenant != null)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
        }
    }
}