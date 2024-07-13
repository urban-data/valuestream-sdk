package com.yesitlabs.adtraking

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.tasks.Task
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Calendar

object Utils {
    /**
     * This function is use for when mobile location is disable of the user mobile
     */
    fun displayLocationSettingsRequest(context: Context, onSuccess: () -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 1000
            numUpdates = 1
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val task: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(context).checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            onSuccess.invoke()
        }
        task.addOnFailureListener { exception ->
            val status = (exception as? ResolvableApiException)?.status
            when (status?.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    Log.i(
                        "Sdk",
                        "Location settings are not satisfied. Show the user a dialog to upgrade location settings"
                    )
                    try {
                        status.resolution?.let {
                            // Show the dialog by calling startIntentSenderForResult(), and check the result in onActivityResult().
                            (context as? Activity)?.startIntentSenderForResult(
                                it.intentSender,
                                100,
                                null,
                                0,
                                0,
                                0,
                                null
                            )
                        }
                    } catch (e: IntentSender.SendIntentException) {
                        Log.i("Sdk", "PendingIntent unable to execute request.")
                    }
                }

                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                    Log.i(
                        "Sdk",
                        "Location settings are inadequate, and cannot be fixed here. Dialog not created."
                    )
            }
        }
    }

    /**
     * This is alert function show when permission is not enable in the app setting
     */
    fun alertBoxLocation(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Alert")
        builder.setMessage(R.string.dialogMessage)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") { _, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            ActivityCompat.startActivityForResult(context as Activity, intent, 200, null)
        }
        builder.setNeutralButton("Cancel") { _, _ -> }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
        return alertDialog
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun calculateMD5HashEmail(email: String): String {
        try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(email.toByteArray())
            val messageDigest = digest.digest()
            val hexString = java.lang.StringBuilder()
            for (b in messageDigest) {
                val hex = Integer.toHexString(0xFF and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    fun calculateSHA256Hash(phone: String): String {
        try {
            val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
            val hashBytes: ByteArray = digest.digest(phone.toByteArray(StandardCharsets.UTF_8))
            val hexString = StringBuilder()
            for (hashByte in hashBytes) {
                val hex = Integer.toHexString(0xff and hashByte.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    fun timePassedSince(time: Long): String {
        val currentTime = Calendar.getInstance().time.time
        val diff = currentTime - time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return "$hours:$minutes:$seconds"
    }

    fun isGPSEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun isOnline(context: Context?): Boolean {
        context ?: return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getCurrentUnixTimestamp(): String {
        return (System.currentTimeMillis() / 1000L).toString()
    }
}
