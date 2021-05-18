package dev.jesselima.geofences

import com.google.android.gms.maps.model.LatLng

/**
 * Stores latitude and longitude information along with a hint to help user find the location.
 */
data class LandmarkData(
    val id: String,
    val hint: Int,
    val name: Int,
    val latLong: LatLng
)