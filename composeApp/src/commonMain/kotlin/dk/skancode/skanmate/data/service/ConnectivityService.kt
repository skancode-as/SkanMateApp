package dk.skancode.skanmate.data.service

import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ConnectivityService {
    val connectivityMessageChannel: ReceiveChannel<ConnectivityMessage>
    val connectivityMessageResultChannel: SendChannel<ConnectivityMessageResult>
    val connectionFlow: SharedFlow<Boolean>
    val offlineMode: StateFlow<Boolean>

    suspend fun isConnected(): Boolean
    suspend fun enableOfflineMode()
    suspend fun disableOfflineMode()
    suspend fun sendConnectivityMessage(msg: ConnectivityMessage): ConnectivityMessageResult

    companion object {
        val instance: ConnectivityService = ConnectivityServiceInstance
        suspend fun isConnected(): Boolean = instance.isConnected()
        suspend fun enableOfflineMode() = instance.enableOfflineMode()
        suspend fun disableOfflineMode() = instance.disableOfflineMode()
        suspend fun sendConnectivityMessage(msg: ConnectivityMessage): ConnectivityMessageResult = instance.sendConnectivityMessage(msg)
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

    private val _connectivityMessageChannel = Channel<ConnectivityMessage>(capacity = Channel.BUFFERED)
    private val _connectivityMessageResultChannel = Channel<ConnectivityMessageResult>(capacity = Channel.BUFFERED)
    private val _offlineMode = MutableStateFlow(false)
    private val _connectionFlow = MutableSharedFlow<Boolean>(1)

    override val connectivityMessageChannel: ReceiveChannel<ConnectivityMessage>
        get() = _connectivityMessageChannel
    override val connectivityMessageResultChannel: SendChannel<ConnectivityMessageResult>
        get() = _connectivityMessageResultChannel
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

    override suspend fun sendConnectivityMessage(msg: ConnectivityMessage): ConnectivityMessageResult {
        _connectivityMessageChannel.send(msg)
        for (result in _connectivityMessageResultChannel) {
            when(result.message) {
                msg -> {
                    return result
                }
                else -> _connectivityMessageResultChannel.send(result)
            }
        }

        throw IllegalStateException("ConnectivityService::sendConnectivityMessage() - connectivity message result channel was closed unexpectedly")
    }
}