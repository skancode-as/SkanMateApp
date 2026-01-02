package dk.skancode.skanmate.location

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dk.skancode.skanmate.util.unreachable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorized
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val DEFAULT_DISTANCE_FILTER_METERS: Double = 10.0

class LocationViewModel: ViewModel() {
    private val delegate = LocationManagerDelegate()
    val locationFlow: StateFlow<LocationData?>
        get() = delegate.locationFlow
    private val locationManager = CLLocationManager()
    val authorizationStatus: LocationAuthorizationStatus
        get() = delegate.authorizationStatus
    val isAuthorized: Boolean
        get() = delegate.authorizationStatus.isAuthorized

    init {
        delegate.authorizationStatus =
            LocationAuthorizationStatus.from(locationManager.authorizationStatus())
        locationManager.setDelegate(delegate)
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = DEFAULT_DISTANCE_FILTER_METERS
    }

    /**
     * Requests authorization from the user for when in use location updates, if authorization requests are allowed.
     * @return true if the authorization request was allowed, else returns false
     */
    fun tryRequestAuthorization(): Boolean {
        return if (delegate.authorizationStatus == LocationAuthorizationStatus.NotDetermined) {
            locationManager.requestWhenInUseAuthorization()
            true
        } else {
            false
        }
    }

    fun startLocationRequests() {
        if (isAuthorized && !delegate.updatesIsRunning) {
            locationManager.startUpdatingLocation()
        }
    }

    fun stopLocationRequests() {
        if (isAuthorized && delegate.updatesIsRunning) {
            locationManager.stopUpdatingLocation()
        }
    }
}

sealed class LocationAuthorizationStatus(
    val clStatus: CLAuthorizationStatus,
    val isAuthorized: Boolean
) {
    data object NotDetermined : LocationAuthorizationStatus(
        clStatus = kCLAuthorizationStatusNotDetermined,
        isAuthorized = false
    )

    data object Denied :
        LocationAuthorizationStatus(clStatus = kCLAuthorizationStatusDenied, isAuthorized = false)

    data object Restricted : LocationAuthorizationStatus(
        clStatus = kCLAuthorizationStatusRestricted,
        isAuthorized = false
    )

    data object Always : LocationAuthorizationStatus(
        clStatus = kCLAuthorizationStatusAuthorizedAlways,
        isAuthorized = true
    )

    data object InUse : LocationAuthorizationStatus(
        clStatus = kCLAuthorizationStatusAuthorizedWhenInUse,
        isAuthorized = true
    )

    companion object {
        fun from(clStatus: CLAuthorizationStatus): LocationAuthorizationStatus {
            return when (clStatus) {
                NotDetermined.clStatus -> NotDetermined
                Denied.clStatus -> Denied
                Restricted.clStatus -> Restricted
                Always.clStatus -> Always
                InUse.clStatus -> InUse
                else -> unreachable("LocationAuthorizationStatus::fromClStatus($clStatus)")
            }
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
private class LocationManagerDelegate : CLLocationManagerDelegateProtocol, NSObject() {
    private val locationUpdatesIsRunning = AtomicBoolean(false)
    private val _locationFlow = MutableStateFlow<LocationData?>(null)
    val locationFlow: StateFlow<LocationData?>
        get() = _locationFlow
    var authorizationStatus by mutableStateOf(
        LocationAuthorizationStatus.from(
            kCLAuthorizationStatusNotDetermined
        )
    )
    val updatesIsRunning: Boolean
        get() = locationUpdatesIsRunning.load()

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus
    ) = handleAuthorizationChange(manager, didChangeAuthorizationStatus)


    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) =
        handleAuthorizationChange(manager, manager.authorizationStatus)

    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        println("LocationManagerDelegate::locationManager(manager: $manager, didUpdateLocations.size: ${didUpdateLocations.size})")
        didUpdateLocations
            .filterIsInstance<CLLocation>()
            .last()
            .let { cLLocation ->
                cLLocation.coordinate.useContents {
                    _locationFlow.update {
                        LocationData(latitude = latitude, longitude = longitude)
                    }
                }
            }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        println("LocationManagerDelegate::locationManager:didFailWithError(${didFailWithError.localizedDescription})")
    }

    override fun locationManagerDidPauseLocationUpdates(manager: CLLocationManager) {
        println("LocationManagerDelegate::locationManagerDidPauseLocationUpdates($manager)")
        locationUpdatesIsRunning.compareAndSet(expectedValue = true, newValue = false)
    }

    override fun locationManagerDidResumeLocationUpdates(manager: CLLocationManager) {
        println("LocationManagerDelegate::locationManagerDidResumeLocationUpdates($manager)")
        locationUpdatesIsRunning.compareAndSet(expectedValue = false, newValue = true)
    }

    private fun handleAuthorizationChange(manager: CLLocationManager, newAuthorization: CLAuthorizationStatus) {
        println("LocationManagerDelegate::handleAuthorizationChange(${newAuthorization.toAuthorizationString()})")
        authorizationStatus = LocationAuthorizationStatus.from(newAuthorization)

        if (authorizationStatus.isAuthorized && _locationFlow.value == null) {
            manager.requestLocation()
        }
    }
}

private fun CLAuthorizationStatus.toAuthorizationString(): String =
    when (this) {
        kCLAuthorizationStatusNotDetermined -> "NotDetermined"
        kCLAuthorizationStatusDenied -> "Denied"
        kCLAuthorizationStatusRestricted -> "Restricted"
        kCLAuthorizationStatusAuthorizedAlways -> "AuthorizedAlways"
        kCLAuthorizationStatusAuthorizedWhenInUse -> "AuthorizedWhenInUse"
        kCLAuthorizationStatusAuthorized -> "Authorized"
        else -> "Unknown"
    }