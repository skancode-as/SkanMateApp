package dk.skancode.skanmate.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.data.service.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InitializationResult (
    val user: UserModel?,
    val tenant: TenantModel?,
)

class InitializerViewModel(
    authService: AuthService,
): ViewModel() {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean>
        get() = _isInitialized

    private val _initializationResult = mutableStateOf<InitializationResult?>(null)
    val initializationResult: State<InitializationResult?>
        get() = _initializationResult

    init {
        viewModelScope.launch {
            val user = authService.userFlow.first()
            val tenant = authService.tenantFlow.first()
            _initializationResult.value = InitializationResult(
                user = user,
                tenant = tenant,
            )
            _isInitialized.update { true }
        }
    }
}