package dk.skancode.skanmate.location

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    @SerialName("lat") val latitude: Double,
    @SerialName("lng") val longitude: Double,
)