package com.yesitlabs.adtraking

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.uppercase
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object InformationGatherer {

    fun getDeviceType(): String {
        return "Android"
    }

    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getDeviceType(context: Context): String {
        return when (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_LARGE,
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> "tablet"
            else -> "mobile"
        }
    }

     fun getConnectionType(context: Context): String {
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

    fun getIPV4Address(): String {
        return getIPAddress(useIPv4 = true)
    }

    fun getIPV6Address(): String {
        return getIPAddress(useIPv4 = false)
    }

    fun getConnectionProvider(context: Context): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return null
            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wifiManager =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    return if (wifiInfo.ssid != null && wifiInfo.ssid != "<unknown ssid>") {
                        wifiInfo.ssid.replace("\"", "") // Remove quotes around the SSID
                    } else {
                        "Unknown Wi-Fi Network"
                    }
                }

                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val networkInfo = telephonyManager.networkOperatorName
                    networkInfo ?: "Unknown Cellular Network"
                }

                else -> null
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return null
            return when (activeNetworkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> {
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val networkInfo = telephonyManager.networkOperatorName
                    networkInfo ?: "Unknown Cellular Network"
                }

                ConnectivityManager.TYPE_MOBILE -> {
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val networkInfo = telephonyManager.networkOperatorName
                    networkInfo ?: "Unknown Cellular Network"
                }

                else -> null
            }
        }
    }

    fun getConnectionCountryCode(context: Context): String {
        val countryCode: String
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        countryCode = telephonyManager.networkCountryIso
        return countryCode
    }

    suspend fun getPostalCodeAndCountry(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            var postalCode = "No Postal Code Found"
            var country = "No Country Found"
            try {
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.let {
                    if (it.isNotEmpty()) {
                        val address = it[0]
                        if(address.postalCode != null) {
                            postalCode = address.postalCode
                        }

                        if(address.countryCode != null) {
                            country = address.countryName ?: country
                        }
                    }
                }
            } catch (_: Exception) { }
            Pair(postalCode, country)
        }
    }


    @SuppressLint("HardwareIds")
    fun getIMEI(context: Context): String {
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

    fun getBSSID(context: Context): String? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.bssid
    }

    fun getSSID(context: Context): String {
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

    fun getAppName(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "App Name Not Found"
        }
    }

    fun getAppBundleId(context: Context): String {
        return context.packageName
    }

    fun getKeyboardLanguage(context: Context): String {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        val ims = imm?.currentInputMethodSubtype
        return ims?.languageTag ?: "Unknown"
    }

    fun getUserAgent(context: Context): String {
        val webView = android.webkit.WebView(context)
        val settings: WebSettings = webView.settings
        return settings.userAgentString
    }

    @SuppressLint("HardwareIds")
    fun getVendorIdentifier(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "Unknown"
    }

    suspend fun getAdvertisingId(applicationContext: Context): String {
        return withContext(Dispatchers.IO) {
            var advertisingId = ""
            try {
                val adInfo =
                    AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
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

    fun getDeviceBrand(): String? {
        return Build.MANUFACTURER
    }

    fun getDeviceModel(): String? {
        return Build.MODEL
    }

    fun getDeviceOSV() : String? {
        return Build.VERSION.RELEASE
    }
}
