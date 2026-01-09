package dk.skancode.skanmate.data.service

import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ConnectivityService {
    val connectivityMessageFlow: SharedFlow<ConnectivityMessage>
    val connectionFlow: SharedFlow<Boolean>
    val offlineMode: StateFlow<Boolean>

    suspend fun isConnected(): Boolean
    suspend fun enableOfflineMode()
    suspend fun disableOfflineMode()
    suspend fun sendConnectivityMessage(msg: ConnectivityMessage): Flow<ConnectivityMessageResult>
    suspend fun sendConnectivityMessageResult(result: ConnectivityMessageResult)

    companion object {
        val instance: ConnectivityService = ConnectivityServiceInstance
        suspend fun enableOfflineMode() = instance.enableOfflineMode()
        suspend fun sendConnectivityMessage(msg: ConnectivityMessage): Flow<ConnectivityMessageResult> = instance.sendConnectivityMessage(msg)
        suspend fun sendConnectivityMessageResult(result: ConnectivityMessageResult) = instance.sendConnectivityMessageResult(result)
    }
}

sealed interface ConnectivityMessage {
    class RequestTimeout(): ConnectivityMessage
    data class OfflineModeRequested(val enabled: Boolean): ConnectivityMessage
}

sealed interface ConnectivityMessageResult {
    val message: ConnectivityMessage
    data class Accepted(override val message: ConnectivityMessage): ConnectivityMessageResult
    data class Dismissed(override val message: ConnectivityMessage): ConnectivityMessageResult
}

private object ConnectivityServiceInstance: ConnectivityService {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectivity = Connectivity(scope) {
        autoStart = true
    }

    private val _connectivityMessageChannel = MutableSharedFlow<ConnectivityMessage>()
    private val _connectivityMessageResultFlow = MutableSharedFlow<ConnectivityMessageResult>()
    private val _offlineMode = MutableStateFlow(false)
    private val _connectionFlow = MutableSharedFlow<Boolean>(1)

    override val connectivityMessageFlow: SharedFlow<ConnectivityMessage>
        get() = _connectivityMessageChannel
    override val offlineMode: StateFlow<Boolean>
        get() = _offlineMode
    override val connectionFlow: SharedFlow<Boolean>
        get() = _connectionFlow

    init {
        scope.launch {
            connectivity.statusUpdates.collect { status ->
                _connectionFlow.emit(!_offlineMode.value && status.isConnected)
            }
        }
    }

    override suspend fun isConnected(): Boolean =
        !_offlineMode.value && _connectionFlow.first()
    override suspend fun enableOfflineMode() {
        _offlineMode.update { true }
        _connectionFlow.emit(false)
    }
    override suspend fun disableOfflineMode() {
        _offlineMode.update { false }
        _connectionFlow.emit(connectivity.status().isConnected)
    }

    override suspend fun sendConnectivityMessage(msg: ConnectivityMessage) = flow {
       _connectivityMessageChannel.emit(msg)

        emit(
            value = _connectivityMessageResultFlow.first { it.message == msg }
        )
    }

    override suspend fun sendConnectivityMessageResult(result: ConnectivityMessageResult) {
        _connectivityMessageResultFlow.emit(result)
    }
}