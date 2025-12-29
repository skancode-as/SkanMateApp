package dk.skancode.skanmate.location

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AndroidLocationCollector(
    val locationFlow: StateFlow<Location?>,
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
                        locationData = location?.let { loc ->
                            LocationData(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                            )
                        }
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
            val cur = locationFlow.first()
            listener.onLocationCollected(cur?.let { LocationData(it.latitude, it.longitude) })
        }
    }

    override fun removeListener(listener: LocationCollectorListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            stopCollecting()
        }
    }
}