package com.yesitlabs.adtraking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.*
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import okhttp3.internal.uppercase
import retrofit2.Call
import retrofit2.Response
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class Adtraking:AppCompatActivity(){

    companion object {

        private var time: Long = 0
        private lateinit var gender: String
        private lateinit var licensekey: String
        private lateinit var yod: String
        private lateinit var email: String

        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context

        // this function call for Device Type
        private fun getDeviceType(context: Context): String {
            return when (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                Configuration.SCREENLAYOUT_SIZE_LARGE,
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> "tablet"
                else -> "mobile"
            }
        }

        // this function call for Device Model
        private fun getDeviceModel(): String {
            return "${Build.MANUFACTURER} ${Build.MODEL}"
        }

        // Get timestamp in a specific format (e.g., "yyyy-MM-dd HH:mm:ss")
        private var formattedTimestamp = System.currentTimeMillis() / 1000L

        // this function call for Ip Address
        private fun getIPAddress(useIPv4: Boolean): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val ipAddr = addr.hostAddress
                            val isIPv4 = ipAddr!!.indexOf(':') < 0
                            if (useIPv4) {
                                if (isIPv4) return ipAddr
                            } else {
                                if (!isIPv4) {
                                    val delim = ipAddr.indexOf('%') // Remove scope identifier
                                    return if (delim < 0) ipAddr.uppercase() else ipAddr.substring(
                                        0,
                                        delim
                                    ).uppercase()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        // Get IPv4 address
        private var ipv4Address = getIPAddress(useIPv4 = true)

        // Get IPv6 address
        private var ipv6Address = getIPAddress(useIPv4 = false)

        // this function call for Connection Type
        private fun getConnectionType(context: Context): String {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.let {
                return when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                    else -> "No Connection"
                }
            }
            return "No Connection"
        }

        // this function call for device Brand
        private var deviceBrand = Build.MANUFACTURER

        // this function call for device_model_hmv
        private var device_model_hmv = Build.MODEL



        // this line use for deviceOS
//        private val deviceOSV = android.os.Build.VERSION.SDK_INT
        private val deviceOSV = Build.VERSION.RELEASE

        // this function call for ConnectionProvider
        private fun getConnectionProvider(context: Context): String? {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return null
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
                return when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val wifiInfo = wifiManager.connectionInfo
                        return if (wifiInfo.ssid != null && wifiInfo.ssid != "<unknown ssid>") {
                            wifiInfo.ssid.replace("\"", "") // Remove quotes around the SSID
                        } else {
                            "Unknown Wi-Fi Network"
                        }
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        val networkInfo = telephonyManager.networkOperatorName
                        networkInfo ?: "Unknown Cellular Network"
                    }
                    else -> null
                }
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return null
                return when (activeNetworkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        val networkInfo = telephonyManager.networkOperatorName
                        networkInfo ?: "Unknown Cellular Network"
                    }
                    ConnectivityManager.TYPE_MOBILE -> {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        val networkInfo = telephonyManager.networkOperatorName
                        networkInfo ?: "Unknown Cellular Network"
                    }
                    else -> null
                }
            }
        }

        // this function call for Connection Country Code
        private fun getConnectionCountryCode(context: Context): String {
            val countryCode: String
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            countryCode = telephonyManager.networkCountryIso
            return countryCode
        }

        // this function call for postalCode and country
        private suspend fun recievePostalCoadeAndCountry(context: Context, latitude: Double, longitude: Double): String {
            return withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context, Locale.getDefault())
                var country = ""
                var postalCode = ""
                try {
                    val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses.let {
                        if (addresses!!.isNotEmpty()){
                            val address = addresses[0]
                            postalCode = address.postalCode.toString()
                            country = address.countryName.toString()
                        }
                    }
                } catch (e: Exception) {
                    "No Post Code Found " + ":" + "No Country Found"
                }
                "$postalCode:$country"
            }
        }


        /** this function call for start session when app is open then function is call because user
        get the start time to calculate the total duration */
        fun startSession() {
            time=Calendar.getInstance().time.time
        }

        private fun requestLocationPermission(context: Context) {
            // Request the location permission from the user
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101
            )
        }


        /**
         * The onRequestPermissionsResult function is called when the user responds to a permission request. It takes two parameters: requestCode (an integer representing the code for the permission request) and grantResults (an array of integers representing the result for each requested permission).
        Within the function:

        1. It checks if the requestCode is 101, indicating that the permission request pertains to location services.

        2. It verifies if grantResults is not empty and whether the first element in grantResults array indicates that the permission was granted.

        Note: There is an error in the permission check logic.
         * It should use grantResults[0] == PackageManager.PERMISSION_GRANTED, but it incorrectly uses grantResults[0] + grantResults[0] == PackageManager.PERMISSION_GRANTED.

        3. If the permission is granted, it calls the displayLocationSettingsRequest(context) method, which presumably initiates a request to display the location settings.

        4. If the permission is not granted, it calls the alertBoxLocation() method, which likely displays an alert box to the user indicating that location permissions are necessary.
         */

        fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
            // Check if the location permission is granted by the user
            if (requestCode == 101 && grantResults.isNotEmpty() && (grantResults[0] + grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                displayLocationSettingsRequest(context)
            } else {
                alertBoxLocation()
            }
        }


        /**
         *
        The onActivityResult function is called when an activity that was started for a result returns its result. This function takes two parameters: requestCode (an integer identifying the request) and resultCode (an integer representing the result of the activity).
        Within the function:

        1. It first checks if the requestCode is 100, indicating that the result is related to the location permission request.

        2. If the requestCode is 100, it then checks if the resultCode is Activity.RESULT_OK, which indicates that the location permission was successfully granted by the user.

        1. If the resultCode is Activity.RESULT_OK, it calls the apiData() method, which presumably initiates some data fetching or API call that requires location access.
        2. If the resultCode is not Activity.RESULT_OK, it shows a toast message to the user saying "Please turn on location".

        3. If the requestCode is not 100, it calls the displayLocationSettingsRequest(context) method, which likely initiates a request to display the location settings to the user.

         * */
        fun onActivityResult(requestCode: Int, resultCode: Int) {
            // Check if the location permission is granted by the user
            if (requestCode == 100) {
                if (Activity.RESULT_OK == resultCode) {
                    apiData()
                } else {
                    Toast.makeText(context, "Please turn on location", Toast.LENGTH_SHORT).show()
                }
            } else {
                displayLocationSettingsRequest(context)
            }
        }

        /**
         * This is alert function show when permission is not enable in the app setting
         */
        private fun alertBoxLocation() {
            val builder = AlertDialog.Builder(context)
            //set title for alert dialog
            builder.setTitle("Alert")
            //set message for alert dialog
            builder.setMessage(R.string.dialogMessage)
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            //performing positive action
            builder.setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                startActivityForResult(context as Activity, intent, 200, null)
            }
            //performing cancel action
            builder.setNeutralButton("Cancel") { _, _ ->

            }

            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()
        }


        /**
         * This function is use for when mobile location is disable of the user mobile
         */
        private fun displayLocationSettingsRequest(context: Context) {
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
                apiData()
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
         * This function is use for check location permission in the app
         */
        private fun isLocationPermissionGranted(context: Context): Boolean {
            // Check if the location permission is granted
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * this function call for get Session
         */
        private fun getSessionStart(sessionStartTime: Long): String {
            val currentTime = Calendar.getInstance().time.time
            val diff = currentTime - sessionStartTime
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return "$hours:$minutes:$seconds"
        }


        /*
         *this line use for hem
         */
        private fun calculateMD5HashEmail(email: String): String {
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


        /**
         * this function call for imei
         * */
        @SuppressLint("HardwareIds")
        private fun getIMEI(context: Context): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } else {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.imei ?: Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            }
        }

        // this function call for BSSID
        private fun getBSSID(context: Context): String? {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            return wifiInfo?.bssid
        }


        // this function call for SSID
        private fun getSSID(context: Context): String {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                wifiInfo?.let {
                    if ((wifiInfo.ssid != null) && wifiInfo.ssid.isNotEmpty() && (wifiInfo.ssid != "<unknown ssid>")) {
                        return wifiInfo.ssid.replace(
                            "\"",
                            ""
                        ) // Removing surrounding quotes, if any
                    }
                }
            }
            return "SSID not available"
        }

        // this function call for AppName
        private fun getAppName(context: Context): String {
            return try {
                val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                "App Name Not Found"
            }
        }

        // this function call for AppBundleId
        private fun getAppBundleId(context: Context): String {
            return context.packageName
        }

        // this function call for Language
        private fun getKeyboardLanguage(context: Context): String {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            val ims = imm?.currentInputMethodSubtype
            return ims?.languageTag ?: "Unknown"
        }

        // this function call for UserAgent
        private fun getUserAgent(context: Context): String {
            val webView = android.webkit.WebView(context)
            val settings: WebSettings = webView.settings
            return settings.userAgentString
        }

        // this function call for cell_id
        @SuppressLint("HardwareIds")
        private fun getVendorIdentifier(context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "Unknown"
        }


        // this function call for MSISDN
        private fun calculateSHA256Hash(phone:String): String {
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


        /**
         * This function call when all data from the user and click the final function
         */
        @SuppressLint("HardwareIds")
         fun froyoUploadData(context: Context,gender: String, licenseKey: String, yod: String, email: String) {
            this.context=context
            // This is check condition if network is enable then execute the if condition
            if (isOnline(context)) {
                this.gender = gender
                this.licensekey = licenseKey

                // This is date of birth and enter the YYYY/MM/DD format
                this.yod = yod
                this.email = email

                // Check if the location permission is granted
                if (isLocationPermissionGranted(context)) {
                    // Location permission is already granted, and check gps is enable or not if enable the call the api
                    // other wise user show the alert box of location
                    if (isGPSEnabled(context)) {
                        apiData()
                    } else {
                        displayLocationSettingsRequest(context)
                    }
                } else {
                    // Location permission is not granted, request it from the user
                    requestLocationPermission(context)
                }
            } else {
                Toast.makeText(context, "Please check your Internet connection", Toast.LENGTH_SHORT)
                    .show()
            }
        }


        /**
         * This function get the advertising id from the user, if user enable the permission from the ads in the settings
         */
        private suspend fun getAdvertisingId():String {
            return withContext(Dispatchers.IO) {
                var advertisingId = ""
                try {
                    val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
                    val adId = adInfo.id
                    if (adId != null) {
                        advertisingId = adId
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    advertisingId = e.message.toString()
                }
                advertisingId
            }
        }

        // This function is use for check gps enable or not
        private fun isGPSEnabled(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }


        /* This api call when location is enable and get the location from the user mobile device and
         send the all data in the admin */
        @OptIn(DelicateCoroutinesApi::class)
        private  fun apiData() {
            GlobalScope.launch(Dispatchers.Main) {
                val horizontalAccuracy: Float
                val verticalAccuracy: Float
                val speed: Float
                val gpsLat: Double
                val gpsLong: Double
                val altitude: Double
                val locationType: String
                val getPostalCode: String
                val country: String
                val gps = TrackerGps(context)
                val gpsLocation = gps.getLocation()
                if (gpsLocation != null) {
                    horizontalAccuracy = gpsLocation.accuracy
                    verticalAccuracy = gpsLocation.verticalAccuracyMeters
                    gpsLat = gpsLocation.latitude
                    gpsLong = gpsLocation.longitude
                    altitude = gpsLocation.altitude
                    locationType = gpsLocation.provider.toString()
                    speed = gpsLocation.speedAccuracyMetersPerSecond
                    val postalcodcountry = recievePostalCoadeAndCountry(context, gpsLocation.latitude, gpsLocation.longitude)
                    getPostalCode = postalcodcountry.split(":")[0]
                    country = postalcodcountry.split(":")[1]


                    // This is CoroutineScope and get the Advertising Id if user enable the Advertising
                    // from the setting

                    val advertisingId = getAdvertisingId()

                    val deviceModel = getDeviceModel()

                    val maididType = "GAID"

                    val cellId = getVendorIdentifier(context)

                    val userAgent = getUserAgent(context)

                    val language = getKeyboardLanguage(context)

                    val appBundleId = getAppBundleId(context)

                    val appName = getAppName(context)

                    val sSID =  getSSID(context)

                    val bSSID = getBSSID(context)

                    val imei = getIMEI(context)

                    val countryCode = getConnectionCountryCode(context)

                    val connectionProvider = getConnectionProvider(context)

                    val connectionType = getConnectionType(context)

                    val deviceType = getDeviceType(context)

                    val mnc = context.resources.configuration.mnc

                    val mcc = context.resources.configuration.mcc

                    val mSISDN = calculateSHA256Hash("Phone")

                    val hem = calculateMD5HashEmail(email)


                    // Retrieve the default locale/language of the device
                    val currentLocale = Locale.getDefault()

                    val languageDisplayName = currentLocale.getDisplayName(currentLocale)

                    val sessionTime=getSessionStart(time)

                    Log.d("******","session :-"+sessionTime)


                    val apiInterface: Api = RetrofitClient.getClient()!!.create(Api::class.java)

                    val call: Call<ApiModel> = apiInterface.addData(
                        licensekey,
                        deviceType,
                        deviceModel,
                        gpsLat.toString(),
                        gpsLong.toString(),
                        gender,
                        altitude.toString(),
                        maididType,
                        cellId,
                        userAgent,
                        language,
                        appBundleId,
                        appName,
                        sSID,
                        bSSID,
                        imei,
                        hem,
                        locationType,
                        verticalAccuracy.toString(),
                        horizontalAccuracy.toString(),
                        country,
                        countryCode,
                        connectionProvider,
                        deviceOSV.toString(),
                        "android",
                        device_model_hmv,
                        deviceBrand,
                        connectionType,
                        ipv4Address,
                        ipv6Address,
                        getPostalCode,
                        yod,
                        mnc.toString(),
                        mcc.toString(),
                        sessionTime,
                        "",
                        speed.toString(),
                        formattedTimestamp.toString(),
                        mSISDN,
                        advertisingId,
                        languageDisplayName
                    )
                    call.enqueue(object : retrofit2.Callback<ApiModel> {
                        override fun onResponse(call: Call<ApiModel>, response: Response<ApiModel>) {
                            try {
                                if (response.body()!!.success) {
                                    val sendError = ApiModel(
                                        response.body()!!.license_key,
                                        response.body()!!.message,
                                        response.body()!!.success
                                    )
                                    Toast.makeText(context, sendError.message, Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    val sendError = ApiModel(
                                        license_key = "non",
                                        message = "Something went wrong",
                                        success = false
                                    )
                                    Toast.makeText(context, sendError.message, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }catch (e:Exception){
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiModel>, t: Throwable) {
                            val sendError = ApiModel(
                                license_key = "non",
                                message = "Something went wrong",
                                success = false
                            )
                            Toast.makeText(context, sendError.message, Toast.LENGTH_SHORT).show()
                        }
                    })
                }else{
                    Toast.makeText(context, "Api Not working", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // this function is call check user internet or not if network is not enable then return the false
        private fun isOnline(context: Context?): Boolean {
            context ?: return false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

    }

}