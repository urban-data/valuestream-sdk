package com.yesitlabs.addtraking

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.location.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.webkit.WebSettings
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*


class AddTraking{


    companion object {

        var time: Long = 0

        // this function call for Device Id
        @SuppressLint("HardwareIds")
        private fun getDeviceId(context: Context): String {
            return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "UNKNOWN_DEVICE_ID"
        }

        // this function call for Device Type
        /* private fun getDeviceType(context: Context): String {
             var deviceType = DeviceUtils.getDeviceType(context)
             return deviceType.toString()
         }*/

        fun getDeviceType(context: Context): String {
            val screenSize =
                context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

            return when (screenSize) {
                Configuration.SCREENLAYOUT_SIZE_LARGE,
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> "Tablet"
                else -> "Phone"
            }
        }


        // this function call for Device Model
        private fun getDeviceModel(): String {
            return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        }

        // get date from date class
        val currentDate = Date()

        // this function call for Time stamp From Date
        private fun getTimestampFromDate(date: Date, format: String): String {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            return sdf.format(date)
        }

        // Get timestamp in a specific format (e.g., "yyyy-MM-dd HH:mm:ss")
//        private var formattedTimestamp = getTimestampFromDate(currentDate, "yyyy-MM-dd HH:mm:ss")
        private var formattedTimestamp = System.currentTimeMillis() / 1000L

        // this function call for Android version
        private fun getAndroidVersion(): String {
            val release = android.os.Build.VERSION.RELEASE
            val sdkVersion = android.os.Build.VERSION.SDK_INT
            return "Android version: $release (SDK $sdkVersion)"
        }

        // this function call for Ip Address
        private fun getIPAddress(useIPv4: Boolean): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val ipAddr = addr.hostAddress
                            val isIPv4 = ipAddr.indexOf(':') < 0

                            if (useIPv4) {
                                if (isIPv4) return ipAddr
                            } else {
                                if (!isIPv4) {
                                    val delim = ipAddr.indexOf('%') // Remove scope identifier
                                    return if (delim < 0) ipAddr.toUpperCase() else ipAddr.substring(
                                        0,
                                        delim
                                    ).toUpperCase()
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

        private var ipv4Address = getIPAddress(useIPv4 = true) // Get IPv4 address
        private var ipv6Address = getIPAddress(useIPv4 = false) // Get IPv6 address


        // this function call for SSID(Wifi)
        @SuppressLint("ServiceCast")
        private fun getWifiSSID(context: Context): String {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (wifiManager.isWifiEnabled) {
                val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                wifiInfo?.let {
                    if (wifiInfo.ssid != null && wifiInfo.ssid.isNotEmpty() && wifiInfo.ssid != "<unknown ssid>") {
                        return wifiInfo.ssid.replace(
                            "\"",
                            ""
                        ) // Removing surrounding quotes, if any
                    }
                }
            }
            return "No Wi-Fi Connection"
        }

        // this function call for Connection Type
        private fun getConnectionType(context: Context): String {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                networkCapabilities?.let {
                    return when {
                        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                        it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                        else -> "No Connection"
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.let {
                    return when (networkInfo.type) {
                        ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                        ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                        else -> "No Connection"
                    }
                }
            }
            return "No Connection"
        }


        // this function call for device Brand
        private var deviceBrand = Build.MANUFACTURER

        // this function call for device_model_hmv
        private var device_model_hmv = Build.MODEL

        // this function call for deviceOS
        private var deviceOS =
            "Android ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})"

        // this line use for deviceOS
//        private val deviceOSV = android.os.Build.VERSION.SDK_INT
        val deviceOSV = Build.VERSION.RELEASE

        // this function call for ConnectionProvider
        private fun getConnectionProvider(context: Context): String? {
            var connectionProvider = "Not Connected"
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val networkCapabilities =
                        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                    if (networkCapabilities != null) {
                        connectionProvider =
                            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                "Wi-Fi"
                            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                "Mobile Data"
                            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                                "Ethernet"
                            } else {
                                "Other"
                            }
                    }
                } else {
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    if (activeNetwork != null && activeNetwork.isConnected) {
                        connectionProvider =
                            if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                                "Wi-Fi"
                            } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                                "Mobile Data"
                            } else if (activeNetwork.type == ConnectivityManager.TYPE_ETHERNET) {
                                "Ethernet"
                            } else {
                                "Other"
                            }
                    }
                }
            }
            return connectionProvider
        }

        // this function call for Connection Country Code
        private fun getConnectionCountryCode(context: Context): String? {
            var countryCode = ""
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (telephonyManager != null) {
                countryCode = telephonyManager.networkCountryIso
            }
            return countryCode
        }

//        // this function call for country
//        private var country = Locale.getDefault().country


        // this function call for postalCode
        private fun getpostalCode(latitude: Double, longitude: Double, context: Context): String {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address: Address?
            var postalCode = ""

            val addresses: List<Address>? =
                geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses!!.isNotEmpty()) {
                address = addresses[0]
                postalCode = address.postalCode
            } else {
                postalCode = "its not appear"
            }
            return postalCode
        }


        // this function call for start session
        fun StartSession(context: Context): Long {
            GPSTracker(context)
            return Calendar.getInstance().time.time
        }

        // this function call for get Session
        private fun getSessionStart(context: Context, sessionStartTime: Long): String {
            val currentTime = Calendar.getInstance().getTime().time
            val diff = currentTime - sessionStartTime
            val seconds = diff / 1000
            var minutes = seconds / 60
            var hours = minutes / 60
            val days = hours / 24


            val value = "" + hours + ":" + minutes + ":" + seconds
            return value
        }


        // this line use for hem
        private val manufacturer = Build.MANUFACTURER
        private val model = Build.MODEL
        private val version = Build.VERSION.RELEASE
        private var deviceInfo = "Manufacturer: $manufacturer\nModel: $model\nVersion: $version"


        fun calculateMD5HashEmail(email: String): String? {
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


        // this function call for imei
        @SuppressLint("HardwareIds")
        private fun getIMEI(context: Context): String? {
            val deviceId: String
            deviceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } else {
                val mTelephony =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (mTelephony.deviceId != null) {
                    mTelephony.deviceId
                } else {
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                }
            }
            return deviceId
        }

        @SuppressLint("HardwareIds")
        @Throws(SecurityException::class, NullPointerException::class)
        fun getIMEINumber(context: Context): String? {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var imei: String?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(tm != null)
                imei = tm.imei
                //this change is for Android 10 as per security concern it will not provide the imei number.
                if (imei == null) {
                    imei = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                }
            } else {
                assert(tm != null)
                imei = if (tm.deviceId != null && tm.deviceId != "000000000000000") {
                    tm.deviceId
                } else {
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                }
            }
            return imei
        }


        // this function call for BSSID
        private fun getBSSID(context: Context): String {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (wifiManager.isWifiEnabled) {
                val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                wifiInfo?.let {
                    if (wifiInfo.bssid != null && wifiInfo.bssid.isNotEmpty()) {
                        return wifiInfo.bssid
                    }
                }
            }
            return "BSSID not available"
        }

        // this function call for SSID
        private fun getSSID(context: Context): String {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (wifiManager.isWifiEnabled) {
                val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                wifiInfo?.let {
                    if (wifiInfo.ssid != null && wifiInfo.ssid.isNotEmpty() && wifiInfo.ssid != "<unknown ssid>") {
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
            val imm: InputMethodManager? =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

            val ims: InputMethodSubtype? = imm!!.currentInputMethodSubtype

            return ims!!.getLocale()
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

        // this function call for MaidID
        private fun generateMaidID(): String {
            val maidID = UUID.randomUUID().toString()
            return maidID
        }


        // this function call for MSISDN
        private fun calculateSHA256Hash(input: String): String? {
            try {
                val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
                val hashBytes: ByteArray = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
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

        fun getAdvertisingId(context: Context): String? {
            try {
                val advertisingInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                if (advertisingInfo.isLimitAdTrackingEnabled) {
                    // Advertising tracking is disabled
                    return null
                } else {
                    // Advertising tracking is enabled, retrieve the Advertising ID
                    return advertisingInfo.id
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesNotAvailableException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesRepairableException) {
                e.printStackTrace()
            }
            return null
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("HardwareIds")
        fun sendData(
            context: Context,
            gender: String,
            license_key: String,
            yod: String,
            email:String/*, diff: Long*/
        ) {

            if (isOnline(context)) {
                var HorizontalAccuracy: Float = 0.0F
                var Vertical_Accuracy: Float = 0.0F
                var speed: Float = 0.0F
                var gpslat = 0.0
                var gpslong = 0.0
                var altitude = 0.0
                var Location_Type = ""
                var getpostalCode = ""
                var country:String = ""
                var session = getSessionStart(context, time)

                var gps = GPSTracker(context)

                var timeDelta:Long=0


                if (gps.getLocation() != null) {
                    HorizontalAccuracy = gps.getLocation().accuracy
                    Vertical_Accuracy = gps.getLocation().verticalAccuracyMeters
                    gpslat = gps.getLatitude()
                    gpslong = gps.getLongitude()
                    altitude = gps.getLocation().altitude
                    Location_Type = gps.getLocation().provider.toString()
                    getpostalCode = getpostalCode(gps.getLatitude(), gps.getLongitude(), context)
                    speed = gps.getLocation().speedAccuracyMetersPerSecond
                } else {
                    HorizontalAccuracy = 0.0f
                    Vertical_Accuracy = 0.0f
                    speed = 0.0f
                    gpslat = 0.0
                    gpslong = 0.0
                    altitude = 0.0
                    Location_Type = ""
                    getpostalCode = ""
                }


                val deviceModel = getDeviceModel()
//                val MaidID = generateMaidID()
                val MaidID = "GAID"
                val cell_id = getVendorIdentifier(context)
                val UserAgent = getUserAgent(context)
                val Language = getKeyboardLanguage(context)
                val AppBundleId = getAppBundleId(context)
                val AppName = getAppName(context)
                val SSID = getSSID(context)
                val BSSID = getBSSID(context)
                country=getCountry(context,gps.getLatitude(),gps.getLongitude())

                val imei = getIMEI(context)
                val Country_Code = getConnectionCountryCode(context)
                val ConnectionProvider = getConnectionProvider(context)
                val Connection_Type = getConnectionType(context)
                val Device_Type = getDeviceType(context)
                val mnc = context.resources.configuration.mnc
                val mcc = context.resources.configuration.mcc
                val MSISDN = calculateSHA256Hash("Phone")

                val advertisingId = gps.AAID()

                val hem=calculateMD5HashEmail(email)

                // Retrieve the default locale/language of the device
                val currentLocale = Locale.getDefault()
                val language = currentLocale.language // This will give you the language code

                val languageDisplayName = currentLocale.getDisplayName(currentLocale)

                val apiInterface: Api = RetrofitClient.getClient()!!.create(Api::class.java)

                val call: Call<ApiModel> = apiInterface.addData(
                    license_key,
                    Device_Type,
                    deviceModel,
                    gpslat.toString(),
                    gpslong.toString(),
                    gender,
                    altitude.toString(),
                    MaidID,
                    cell_id,
                    UserAgent,
                    Language,
                    AppBundleId,
                    AppName,
                    SSID,
                    BSSID,
                    imei,
                    hem,
                    Location_Type.toString(),
                    Vertical_Accuracy.toString(),
                    HorizontalAccuracy.toString(),
                    country,
                    Country_Code,
                    ConnectionProvider,
                    deviceOSV.toString(),
                    deviceOS,
                    device_model_hmv,
                    deviceBrand,
                    Connection_Type,
                    ipv4Address,
                    ipv6Address,
                    getpostalCode,
                    yod,
                    mnc.toString(),
                    mcc.toString(),
                    session,
                    "",
                    speed.toString(),
                    formattedTimestamp.toString(),
                    MSISDN,
                    advertisingId,
                    languageDisplayName
                )
                call.enqueue(object : retrofit2.Callback<ApiModel> {
                    override fun onResponse(call: Call<ApiModel>, response: Response<ApiModel>) {
                        if (response.body()!!.success) {
                            var sendError: ApiModel = ApiModel(
                                response.body()!!.license_key,
                                response.body()!!.message,
                                response.body()!!.success
                            )
                            Toast.makeText(context, sendError.message, Toast.LENGTH_SHORT).show()
                        } else {
                            var sendError: ApiModel = ApiModel(
                                license_key = "non",
                                message = "Something went wrong",
                                success = false
                            )
                            Toast.makeText(context, sendError.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiModel>, t: Throwable) {
                        var sendError: ApiModel = ApiModel(
                            license_key = "non",
                            message = "Something went wrong",
                            success = false
                        )
                        Toast.makeText(context, sendError.message, Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(context, "Please check your Internet connection", Toast.LENGTH_SHORT)
                    .show()
            }
        }



        private fun isOnline(context: Context?): Boolean {
            if (context != null) {
                val connectivity =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (connectivity != null) {
                    val info = connectivity.allNetworkInfo
                    if (info != null) for (networkInfo in info) if (networkInfo.state == NetworkInfo.State.CONNECTED) {
                        return true
                    }
                }
            }
            return false
        }

        private fun getCountry(context: Context,lat:Double,longi:Double):String{
            var countryName:String=""
            var addresses: List<Address>? = null
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                addresses = geocoder.getFromLocation(lat, longi, 1)
                println("add in string " + addresses!!.toTypedArray().toString())
                countryName = addresses!![0].countryName
                val countryCode = addresses!![0].countryCode
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            return countryName
        }
    }




}