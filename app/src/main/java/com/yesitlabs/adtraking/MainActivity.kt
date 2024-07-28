package com.yesitlabs.adtraking

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val licenseKey = "07nYbS65pi4b0jUV"
        val gender = "male"
        val yearOfBirth = "1990"
        val email = "example@example.com"

        AdTraking.setUserDetails(email, yearOfBirth, gender)
        AdTraking.initialize(this, licenseKey, 1)

        this.runOnUiThread {
            Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show()
        }
    }
}