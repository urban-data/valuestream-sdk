package com.example.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.urbandata.pixelsdk.PixelSDK

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.values.all { it }
            if (allPermissionsGranted) {
                initializePixelSDK()
            } else {
                Toast.makeText(
                    this,
                    "Permissions required for app functionality",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions directly
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_NUMBERS
            )
        )
    }

    private fun initializePixelSDK() {
        val licenseKey = "b633-eb76-e5ae-4eef"
        val intervalInMinutes : Long = 1
        val gender = "male"
        val yearOfBirth = "1990"
        val email = "example@example.com"

        PixelSDK.setUserDetails(email, yearOfBirth, gender)
        PixelSDK.initialize(this, licenseKey, intervalInMinutes)

        runOnUiThread {
            Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show()
        }
    }
}
