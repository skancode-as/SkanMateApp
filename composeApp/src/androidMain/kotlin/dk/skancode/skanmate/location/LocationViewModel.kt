package dk.skancode.skanmate.location

import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import dev.icerock.moko.permissions.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class LocationViewModel(
    locationPermission: PermissionState,
    val fusedLocationClient: FusedLocationProviderClient,
): ViewModel(), LocationListener {
    private val _locationFlow = MutableStateFlow<LocationData?>(null)
    val locationFlow: StateFlow<LocationData?>
        get() = _locationFlow
    private val isRequestingLocation = AtomicBoolean(false)
    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000).build()

    init {
        println("LocationViewModel::init(${locationPermission.name})")

        when (locationPermission) {
            PermissionState.Granted -> {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        //println("LocationViewModel::init - lastLocation: (lat: ${lastLocation.latitude}, lng: ${lastLocation.longitude})")
                        _locationFlow.update { lastLocation?.let { LocationData(it.latitude, it.longitude) } }
                    }
                } catch (_: SecurityException) {
                    println("LocationViewModel::init - Location permission was not granted, when accessing lastLocation")
                }
            }
            else -> {}
        }
    }

    fun startLocationRequests() {
        println("LocationViewModel::startLocationRequests()")
        if (isRequestingLocation.compareAndSet(expectedValue = false, newValue = true)) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    this,
                    Looper.getMainLooper(),
                )
            } catch (_: SecurityException) {
                println("LocationViewModel::startLocationRequests -Location permission were not granted when starting Location requests")
            }
        }
    }

    fun stopLocationRequests() {
        println("LocationViewModel::stopLocationRequests()")
        if (isRequestingLocation.compareAndSet(expectedValue = true, newValue = false)) {
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    override fun onLocationChanged(location: Location) {
        println("LocationViewModel::onLocationChanged($location)")
        _locationFlow.update { location.let { LocationData(it.latitude, it.longitude) } }
    }
}