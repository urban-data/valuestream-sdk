package com.example.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.urbandata.pixelsdk.PixelSDK

class MainActivity : AppCompatActivity() {

    // Optional: Use this to request permissions before initializing the SDK.
    // The SDK will use whatever permissions are granted - all are optional.
    // Customize the permissions array based on what data you want to collect.
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
//            // Initialize SDK after permission dialog is dismissed.
//            // Works with whatever was granted (or nothing).
//            initializePixelSDK()
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default: Initialize SDK without requesting any permissions.
        // The SDK will collect non-sensitive data only.
        initializePixelSDK()

        // Alternative: Request permissions first, then initialize SDK in callback.
        // Uncomment the requestPermissionLauncher above and the code below,
        // and comment out the initializePixelSDK() call above.
//        requestPermissionLauncher.launch(
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.READ_PHONE_STATE,
//                Manifest.permission.READ_SMS,
//                Manifest.permission.READ_PHONE_NUMBERS
//            )
//        )
    }

    private fun initializePixelSDK() {
        val licenseKey = "4fe9-f828-30a3-9b86"
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
