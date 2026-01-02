package dk.skancode.skanmate.location

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

interface LocationCollector {
    fun addListener(listener: LocationCollectorListener)
    fun removeListener(listener: LocationCollectorListener)
}

private object StubLocationCollector: LocationCollector {
    override fun addListener(listener: LocationCollectorListener) { }
    override fun removeListener(listener: LocationCollectorListener) { }
}

val LocalLocationCollector: ProvidableCompositionLocal<LocationCollector> = compositionLocalOf { StubLocationCollector }
