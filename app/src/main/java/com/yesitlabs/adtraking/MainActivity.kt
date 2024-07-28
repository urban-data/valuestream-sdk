package com.yesitlabs.adtraking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.values.all { it }
            if (allPermissionsGranted) {
                initializeAdTracking()
            } else {
                Toast.makeText(this, "Permissions required for app functionality", Toast.LENGTH_SHORT).show()
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

    private fun initializeAdTracking() {
        val licenseKey = "07nYbS65pi4b0jUV"
        val gender = "male"
        val yearOfBirth = "1990"
        val email = "example@example.com"

        AdTraking.setUserDetails(email, yearOfBirth, gender)
        AdTraking.initialize(this, licenseKey, 1)

        runOnUiThread {
            Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show()
        }
    }
}
