package com.urbandata.pixelsdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.urbandata.pixelsdk.Utils.isPermissionGranted
import com.urbandata.pixelsdk.Utils.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

// maybe make this a non-singleton later if need be
object TrackerGps {
    private lateinit var contextRef: WeakReference<Context>
    private lateinit var locationManager: LocationManager
    private var locationMinTimeMS: Long = 5000
    private var locationMinDistanceM: Float = 0F

    fun initialize(ctx: Context) {
        contextRef = WeakReference(ctx)
        locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // Suppressing MissingPermission as Android Studio doesn't understand I actually do check for permissions
    @SuppressLint("MissingPermission")
    suspend fun getLocation(): Location? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val ctx = contextRef.get()
            if (ctx == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            val isCoarseLocationEnabled =
                isPermissionGranted(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            val isFineLocationEnabled =
                isPermissionGranted(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION)

            val noPermissions = (!isFineLocationEnabled && !isCoarseLocationEnabled);
            val noProviders = (!hasGps && !hasNetwork)
            val invalidPermissionProviderCase = (!hasNetwork && !isFineLocationEnabled)

            if (noPermissions || noProviders || invalidPermissionProviderCase) {
                if (noPermissions) {
                    logError("Neither ACCESS_COARSE_LOCATION nor ACCESS_FINE_LOCATION permissions were provided!")
                } else if (noProviders) {
                    logError("Neither the network nor the GPS provider is enabled!")
                } else {
                    logError("The GPS provider is enabled, but permissions required aren't! Likewise, the permissions for network provider are enabled, but the network provider isn't!")
                }
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            var currentLocation: Location? = null

            val locationListener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    continuation.resume(getBetterLocation(currentLocation, location))
                    currentLocation = location;
                    locationManager.removeUpdates(this)
                }
            }

            if (hasGps && isFineLocationEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    locationMinTimeMS,
                    locationMinDistanceM,
                    locationListener
                )
            } else { // this means network provider and coarse location permission is enabled
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    locationMinTimeMS,
                    locationMinDistanceM,
                    locationListener
                )
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(locationListener)
            }
        }
    }

    private fun getBetterLocation(
        locationByGps: Location?,
        locationByNetwork: Location?
    ): Location? {
        return if (locationByGps != null && locationByNetwork != null) {
            if (locationByGps.accuracy > locationByNetwork.accuracy) locationByGps else locationByNetwork
        } else {
            locationByGps ?: locationByNetwork
        }
    }

}