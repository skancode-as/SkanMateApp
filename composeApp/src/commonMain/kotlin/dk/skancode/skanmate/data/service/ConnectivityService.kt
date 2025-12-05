package dk.skancode.skanmate.data.service

import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface ConnectivityService {
    val connectionFlow: SharedFlow<Boolean>

    suspend fun isConnected(): Boolean

    companion object {
        val instance: ConnectivityService = ConnectivityServiceInstance
    }
}

private object ConnectivityServiceInstance: ConnectivityService {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectivity = Connectivity(scope) {
        autoStart = true
    }
    private val _connectionFlow = MutableSharedFlow<Boolean>(1)

    override val connectionFlow: SharedFlow<Boolean>
        get() = _connectionFlow

    init {
        scope.launch {
            connectivity.statusUpdates.collect { status ->
                _connectionFlow.emit(status.isConnected)
            }
        }
    }

    override suspend fun isConnected(): Boolean = _connectionFlow.first()
}