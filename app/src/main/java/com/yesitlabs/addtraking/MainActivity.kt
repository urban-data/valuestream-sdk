package com.yesitlabs.addtraking

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.*


class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AddTraking.StartSession(this)

        /*var  tc= findViewById<TextView>(R.id.tv_session)


        AddTraking.time=AddTraking.StartSession(this)

        tc.setOnClickListener(View.OnClickListener {
            AddTraking.sendData(this,"Male","AGN7iEKQvr6ho79L","2023-01-05","yesitlabs@gmail.com"*//*,diff*//*)
        })*/

    }

}