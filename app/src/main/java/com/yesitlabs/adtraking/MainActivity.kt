package com.yesitlabs.adtraking

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var textView : TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.tv_session)

        val context = this
        val adtraking = Adtraking(context)

        // Replace these with actual values
        val gender = "male"
        val licenseKey = "your_license_key"
        val yearOfBirth = "1990"
        val email = "example@example.com"

        adtraking.sendData(gender, licenseKey, yearOfBirth, email)
    }

}