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
import android.telephony.CellInfoGsm
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.uppercase
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import android.location.Location
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.webkit.WebView
import com.yesitlabs.adtraking.Utils.isPermissionGranted
import com.yesitlabs.adtraking.Utils.logError
import com.yesitlabs.adtraking.Utils.md5Hash
import kotlinx.coroutines.tasks.await

object InformationGatherer {
    fun getDeviceType(context: Context): String {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        return when {
            configuration.smallestScreenWidthDp >= 600 -> "Tablet"

            screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                    screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE -> "Tablet"

            else -> "Mobile"
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
                else -> ""
            }
        }
        return ""
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

    fun getConnectionProvider(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return ""
        return when (activeNetworkInfo.type) {
            ConnectivityManager.TYPE_WIFI -> {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val networkInfo = telephonyManager.networkOperatorName
                networkInfo ?: ""
            }

            ConnectivityManager.TYPE_MOBILE -> {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val networkInfo = telephonyManager.networkOperatorName
                networkInfo ?: ""
            }

            else -> ""
        }
    }

    fun getConnectionCountryCode(context: Context): String {
        val countryCode: String
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        countryCode = telephonyManager.networkCountryIso
        return countryCode
    }

    suspend fun getCountry(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            var country = ""
            var countryCode = ""
            try {
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.let {
                    if (it.isNotEmpty()) {
                        val address = it[0]
                        if (address.countryCode != null) {
                            country = address.countryName
                            countryCode = address.countryCode
                        }
                    }
                }
            } catch (_: Exception) {
            }
            Pair(country, countryCode)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getBearingTo(context: Context, targetLat: Double, targetLon: Double): Float? {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        if (isPermissionGranted(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            && isPermissionGranted(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            return null
        }

        val currentLocation = fusedLocationClient.lastLocation.await()

        if (currentLocation != null) {
            val targetLocation = Location("").apply {
                latitude = targetLat
                longitude = targetLon
            }

            return currentLocation.bearingTo(targetLocation)
        }
        return null
    }


    @SuppressLint("HardwareIds")
    fun getHashedIMEI(context: Context): String {
        if (!isPermissionGranted(context, android.Manifest.permission.READ_PHONE_STATE)) return ""
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return md5Hash(telephonyManager.imei)
        } catch (e: Exception) {
            logError("Exception: ${e.message}")
            return ""
        }
    }

    fun getBSSID(context: Context): String {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.bssid ?: ""
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
        return ""
    }

    fun getAppName(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun getAppBundleId(context: Context): String {
        return context.packageName
    }

    fun getKeyboardLanguage(context: Context): String {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        val ims = imm?.currentInputMethodSubtype
        return ims?.languageTag ?: ""
    }

    @SuppressLint("HardwareIds")
    fun getHashedAndroidID(context: Context): String {
        return md5Hash(
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        )
    }

    suspend fun getMAID(applicationContext: Context): String {
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

    fun getMaidType(): String {
        return "GAID"
    }

    fun getDeviceBrand(): String? {
        return Build.BRAND
    }

    fun getDeviceModel(): String? {
        return Build.MODEL
    }

    fun getDeviceHardware(): String? {
        return Build.HARDWARE
    }

    fun getDeviceOS(): String {
        return "Android ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
    }

    fun getDeviceOSV(): String {
        return Build.VERSION.RELEASE
    }

    fun getCurrentLocaleLanguage(): String {
        val currentLocale = Locale.getDefault()
        return currentLocale.getDisplayName(currentLocale)
    }

    fun getMNC(ctx: Context): String {
        return ctx.resources.configuration.mnc.toString()
    }

    fun getMCC(ctx: Context): String {
        return ctx.resources.configuration.mcc.toString()
    }

    suspend fun getUserAgent(context: Context): String = withContext(Dispatchers.Main) {
        val webView = WebView(context)
        val settings: WebSettings = webView.settings
        settings.userAgentString
    }

    @SuppressLint("MissingPermission")
    fun getCellLac(ctx: Context): String {
        if (!isPermissionGranted(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return ""
        }

        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val cellInfoList = telephonyManager.allCellInfo
        for (cellInfo in cellInfoList) {
            if (cellInfo is CellInfoGsm) {
                val cellIdentity = cellInfo.cellIdentity
                return cellIdentity.lac.toString()
            }
        }

        return ""
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun getHashedMSISDN(ctx: Context): String {
        if (!isPermissionGranted(
                ctx,
                android.Manifest.permission.READ_PHONE_STATE
            ) || !isPermissionGranted(
                ctx,
                android.Manifest.permission.READ_SMS
            ) || !isPermissionGranted(ctx, android.Manifest.permission.READ_PHONE_NUMBERS)
        ) return ""

        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return md5Hash(telephonyManager.line1Number)
    }

    @SuppressLint("MissingPermission")
    fun getCellId(ctx: Context): String {
        if (!isPermissionGranted(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION)
            && !isPermissionGranted(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && !isPermissionGranted(ctx, android.Manifest.permission.READ_PHONE_STATE))
        ) {
            return ""
        }

        val telephonyManager =
            ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val cellInfoList: List<CellInfo> = telephonyManager.allCellInfo

        for (cellInfo in cellInfoList) {
            if (cellInfo is CellInfoLte) {
                val cellIdentityLte = cellInfo.cellIdentity
                val cellId = cellIdentityLte.ci
                return cellId.toString()
            }
        }

        return ""
    }
}
