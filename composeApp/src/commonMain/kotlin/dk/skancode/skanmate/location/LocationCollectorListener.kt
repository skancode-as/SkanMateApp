package dk.skancode.skanmate.location

fun interface LocationCollectorListener {
    fun onLocationCollected(locationData: LocationData?)
}