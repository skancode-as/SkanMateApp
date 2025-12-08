package dk.skancode.skanmate.ui.viewmodel

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.service.ConnectivityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectivityViewModel(
    private val connectivityService: ConnectivityService = ConnectivityService.instance,
): ViewModel() {
    private val _connectionFlow = MutableStateFlow(true)
    val connectionFlow: StateFlow<Boolean>
        get() = _connectionFlow

    init {
        viewModelScope.launch {
            connectivityService.connectionFlow.collect { connection ->
                _connectionFlow.update { connection }
            }
        }
    }
}

val LocalConnectionState: ProvidableCompositionLocal<State<Boolean>> = compositionLocalOf { mutableStateOf(true) }