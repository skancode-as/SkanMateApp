package dk.skancode.skanmate.ui.viewmodel

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.service.ConnectivityMessage
import dk.skancode.skanmate.data.service.ConnectivityMessageResult
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
    val offlineModeFlow: StateFlow<Boolean>
        get() = connectivityService.offlineMode

    val dialogMessageFlow = MutableStateFlow<ConnectivityMessage?>(null)

    init {
        viewModelScope.launch {
            connectivityService.connectionFlow.collect { connection ->
                _connectionFlow.update { connection }
            }
        }
        viewModelScope.launch {
            connectivityService.connectivityMessageFlow.collect { message ->
                dialogMessageFlow.update { message }
            }
        }
    }

    fun enableOfflineMode() {
        viewModelScope.launch {
            connectivityService
                .sendConnectivityMessage(ConnectivityMessage.OfflineModeRequested(true))
                .collect { res ->
                    if (res is ConnectivityMessageResult.Accepted) {
                        connectivityService.enableOfflineMode()
                    }
                }
        }
    }
    fun disableOfflineMode() {
        viewModelScope.launch {
            connectivityService
                .sendConnectivityMessage(ConnectivityMessage.OfflineModeRequested(false))
                .collect { res ->
                    if (res is ConnectivityMessageResult.Accepted) {
                        connectivityService.disableOfflineMode()
                    }
                }
        }
    }
}

val LocalConnectionState: ProvidableCompositionLocal<State<Boolean>> = compositionLocalOf { mutableStateOf(true) }
val LocalConnectivityViewModel: ProvidableCompositionLocal<ConnectivityViewModel> = compositionLocalOf { ConnectivityViewModel() }
