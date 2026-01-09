package dk.skancode.skanmate.util

import dk.skancode.skanmate.data.service.ConnectivityMessage
import dk.skancode.skanmate.data.service.ConnectivityMessageResult
import dk.skancode.skanmate.data.service.ConnectivityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun assert(condition: Boolean, msg: String? = null) {
    if (!condition) {
        throw AssertionError(msg ?: "Assertion failed")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun <reified T> withConnectivityTimeout(timeout: Duration, noinline block: suspend CoroutineScope.() -> T): T? = coroutineScope {
    val deferred = async { block() }

    val resultChannel = produce {
        send(deferred.await())
    }
    val timeoutChannel = produce {
        delay(duration = timeout)
        launch {
            ConnectivityService
                .sendConnectivityMessage(msg = ConnectivityMessage.RequestTimeout())
                .collect { result ->
                    when(result) {
                        is ConnectivityMessageResult.Accepted -> {
                            ConnectivityService.enableOfflineMode()
                            send(null)
                        }
                        is ConnectivityMessageResult.Dismissed -> {
                            println("withConnectivityTimeout - Connectivity dialog was dismissed")
                        }
                    }
                }
        }
    }

    select {
        resultChannel.onReceiveCatching {
            it.onSuccess {
                timeoutChannel.cancel()
            }.getOrNull()
        }
        timeoutChannel.onReceiveCatching {
            it.onSuccess {
                deferred.cancel()
                resultChannel.cancel()
            }.getOrNull()
        }
    }
}

sealed class DefaultTimeouts {
    companion object {
        val tableRowSubmit: Duration = 5.seconds
    }
}
