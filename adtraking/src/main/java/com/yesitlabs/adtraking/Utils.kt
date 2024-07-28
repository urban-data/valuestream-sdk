package com.yesitlabs.adtraking

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import com.yesitlabs.adtraking.InformationGatherer.getAppBundleId
import com.yesitlabs.adtraking.InformationGatherer.getAppName
import com.yesitlabs.adtraking.InformationGatherer.getBSSID
import com.yesitlabs.adtraking.InformationGatherer.getBearingTo
import com.yesitlabs.adtraking.InformationGatherer.getCellId
import com.yesitlabs.adtraking.InformationGatherer.getCellLac
import com.yesitlabs.adtraking.InformationGatherer.getConnectionProvider
import com.yesitlabs.adtraking.InformationGatherer.getConnectionType
import com.yesitlabs.adtraking.InformationGatherer.getCountry
import com.yesitlabs.adtraking.InformationGatherer.getCurrentLocaleLanguage
import com.yesitlabs.adtraking.InformationGatherer.getDeviceBrand
import com.yesitlabs.adtraking.InformationGatherer.getDeviceHardware
import com.yesitlabs.adtraking.InformationGatherer.getDeviceModel
import com.yesitlabs.adtraking.InformationGatherer.getDeviceOS
import com.yesitlabs.adtraking.InformationGatherer.getDeviceOSV
import com.yesitlabs.adtraking.InformationGatherer.getDeviceType
import com.yesitlabs.adtraking.InformationGatherer.getHashedAndroidID
import com.yesitlabs.adtraking.InformationGatherer.getHashedIMEI
import com.yesitlabs.adtraking.InformationGatherer.getHashedMSISDN
import com.yesitlabs.adtraking.InformationGatherer.getIPV4Address
import com.yesitlabs.adtraking.InformationGatherer.getIPV6Address
import com.yesitlabs.adtraking.InformationGatherer.getKeyboardLanguage
import com.yesitlabs.adtraking.InformationGatherer.getMAID
import com.yesitlabs.adtraking.InformationGatherer.getMCC
import com.yesitlabs.adtraking.InformationGatherer.getMNC
import com.yesitlabs.adtraking.InformationGatherer.getMaidType
import com.yesitlabs.adtraking.InformationGatherer.getSSID
import com.yesitlabs.adtraking.InformationGatherer.getUserAgent
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Calendar

object Utils {
    var SDK_LOG_TAG: String = "AdTraking"

    fun isPermissionGranted(ctx: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun md5Hash(str: String): String {
        try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(str.toByteArray())
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
            logError(e.message ?: "")
            return ""
        }
    }

    fun timePassedSince(time: Long): String {
        val currentTime = Calendar.getInstance().time.time
        val diff = currentTime - time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return "$hours:$minutes:$seconds"
    }

    fun getCurrentUnixTimestamp(): String {
        return (System.currentTimeMillis() / 1000L).toString()
    }

    fun logError(msg: String) {
        Log.e(SDK_LOG_TAG, msg);
    }

    fun logInfo(msg: String) {
        Log.i(SDK_LOG_TAG, msg);
    }

    suspend fun getStaticData(ctx: Context, adTrakingParams: AdTrakingParams): AdTrakingParams {
        adTrakingParams.device_type = getDeviceType(ctx)
        adTrakingParams.device_os = getDeviceOS()
        adTrakingParams.device_osv = getDeviceOSV()
        adTrakingParams.device_brand = getDeviceBrand() ?: ""
        adTrakingParams.device_model = getDeviceModel() ?: ""
        adTrakingParams.device_model_hmv = getDeviceHardware() ?: ""
        val location = TrackerGps.getLocation()

        if (location != null) {
            val (country, countryCode) = getCountry(ctx, location.latitude, location.longitude)
            adTrakingParams.latitude = location.latitude.toString()
            adTrakingParams.longitude = location.longitude.toString()
            adTrakingParams.country = country
            adTrakingParams.country_code = countryCode
        }

        adTrakingParams.maid = getMAID(ctx)
        adTrakingParams.maid_id = getMaidType()
        adTrakingParams.msisdn = getHashedMSISDN(ctx)
        adTrakingParams.imei = getHashedIMEI(ctx)
        if (adTrakingParams.imei == "") {
            adTrakingParams.imei = getHashedAndroidID(ctx);
        }
        adTrakingParams.app_name = getAppName(ctx)
        adTrakingParams.app_bundle = getAppBundleId(ctx)
        adTrakingParams.cell_id = getCellId(ctx)
        adTrakingParams.cell_lac = getCellLac(ctx)
        adTrakingParams.cell_mnc = getMNC(ctx)
        adTrakingParams.cell_mcc = getMCC(ctx)
        return adTrakingParams
    }

    suspend fun getDynamicData(
        ctx: Context,
        startTime: Long,
        adTrakingParams: AdTrakingParams
    ): AdTrakingParams {
        adTrakingParams.unix_timestamp = getCurrentUnixTimestamp()
        adTrakingParams.connection_type = getConnectionType(ctx)
        adTrakingParams.connection_provider = getConnectionProvider(ctx)
        val location = TrackerGps.getLocation()
        if (location != null) {
            adTrakingParams.latitude = location.latitude.toString()
            adTrakingParams.longitude = location.longitude.toString()
            adTrakingParams.altitude = location.altitude.toString()
            adTrakingParams.bearing = getBearingTo(ctx, location.latitude, location.longitude).toString()
            adTrakingParams.location_type = location.provider ?: ""
            adTrakingParams.speed = location.speed.toString()
            adTrakingParams.horizontalAccuracy = location.accuracy.toString()
            adTrakingParams.verticalAccuracyMeters = location.verticalAccuracyMeters.toString()
        }

        adTrakingParams.session_duration = timePassedSince(startTime)
        adTrakingParams.language = getCurrentLocaleLanguage()
        adTrakingParams.ipv4 = getIPV4Address()
        adTrakingParams.ipv6 = getIPV6Address()
        adTrakingParams.bssid = getBSSID(ctx)
        adTrakingParams.ssid = getSSID(ctx)
        adTrakingParams.keyboard_language = getKeyboardLanguage(ctx)
        adTrakingParams.useragent = getUserAgent(ctx)

        return adTrakingParams
    }
}
