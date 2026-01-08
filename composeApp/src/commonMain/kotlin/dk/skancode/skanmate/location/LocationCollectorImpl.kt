package dk.skancode.skanmate.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocationCollectorImpl(
    val locationFlow: StateFlow<LocationData?>,
    val startCollecting: () -> Unit,
    val stopCollecting: () -> Unit,
    val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): LocationCollector {
    private val listeners = mutableSetOf<LocationCollectorListener>()

    init {
        externalScope.launch {
            locationFlow.collect { location ->
                listeners.forEach { listener ->
                    listener.onLocationCollected(
                        locationData = location,
                    )
                }
            }
        }
    }

    override fun addListener(listener: LocationCollectorListener) {
        if (listeners.isEmpty()) {
            startCollecting()
        }
        listeners.add(listener)

        externalScope.launch {
            val locationData = locationFlow.first()
            listener.onLocationCollected(locationData)
        }
    }

    override fun removeListener(listener: LocationCollectorListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            stopCollecting()
        }
    }
}