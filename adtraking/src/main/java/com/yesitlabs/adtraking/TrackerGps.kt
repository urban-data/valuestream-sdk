package com.yesitlabs.adtraking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class TrackerGps(private val mContext: Context) {

    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null
    private var latitude: Double? = null
    private var longitude: Double? = null


    suspend fun getLocation(): Location = suspendCancellableCoroutine { continuation ->
        locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        var locationByGps: Location? = null
        var locationByNetwork: Location? = null


        val gpsLocationListener = object : android.location.LocationListener {
            override fun onLocationChanged(p0: Location) {
                locationByGps = p0
                if (locationByGps != null && locationByNetwork != null) {

                    continuation.resume(getBetterLocation(locationByGps, locationByNetwork)!!)
                    locationManager.removeUpdates(this)
                }
            }
        }

        val networkLocationListener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                locationByNetwork = location
                if (locationByGps != null && locationByNetwork != null) {
//                    continuation.resume(getBetterLocation(locationByGps, locationByNetwork)!!)
                    locationManager.removeUpdates(this)
                }
            }
        }

        if (hasGps || hasNetwork) {
            if (hasGps) {
                if (ActivityCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                     return@suspendCancellableCoroutine
                }

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    gpsLocationListener
                )
            }
            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0F,
                    networkLocationListener
                )
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(gpsLocationListener)
                locationManager.removeUpdates(networkLocationListener)
            }

            if (locationByGps != null && locationByNetwork != null) {
                continuation.resume(getBetterLocation(locationByGps, locationByNetwork)!!)
            }
        } else {
           // startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

            continuation.resumeWithException(Exception("GPS and Network providers are disabled"))
        }
    }

    private fun getBetterLocation(locationByGps: Location?, locationByNetwork: Location?): Location? {
        return if (locationByGps != null && locationByNetwork != null) {
            if (locationByGps.accuracy > locationByNetwork.accuracy) locationByGps else locationByNetwork
        } else {
            locationByGps ?: locationByNetwork
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

 }