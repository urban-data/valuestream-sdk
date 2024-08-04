package com.urbandata.pixelsdk

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import com.urbandata.pixelsdk.InformationGatherer.getAppBundleId
import com.urbandata.pixelsdk.InformationGatherer.getAppName
import com.urbandata.pixelsdk.InformationGatherer.getBSSID
import com.urbandata.pixelsdk.InformationGatherer.getBearingTo
import com.urbandata.pixelsdk.InformationGatherer.getCellId
import com.urbandata.pixelsdk.InformationGatherer.getCellLac
import com.urbandata.pixelsdk.InformationGatherer.getConnectionProvider
import com.urbandata.pixelsdk.InformationGatherer.getConnectionType
import com.urbandata.pixelsdk.InformationGatherer.getCountry
import com.urbandata.pixelsdk.InformationGatherer.getCurrentLocaleLanguage
import com.urbandata.pixelsdk.InformationGatherer.getDeviceBrand
import com.urbandata.pixelsdk.InformationGatherer.getDeviceHardware
import com.urbandata.pixelsdk.InformationGatherer.getDeviceModel
import com.urbandata.pixelsdk.InformationGatherer.getDeviceOS
import com.urbandata.pixelsdk.InformationGatherer.getDeviceOSV
import com.urbandata.pixelsdk.InformationGatherer.getDeviceType
import com.urbandata.pixelsdk.InformationGatherer.getHashedAndroidID
import com.urbandata.pixelsdk.InformationGatherer.getHashedIMEI
import com.urbandata.pixelsdk.InformationGatherer.getHashedMSISDN
import com.urbandata.pixelsdk.InformationGatherer.getIPV4Address
import com.urbandata.pixelsdk.InformationGatherer.getIPV6Address
import com.urbandata.pixelsdk.InformationGatherer.getKeyboardLanguage
import com.urbandata.pixelsdk.InformationGatherer.getMAID
import com.urbandata.pixelsdk.InformationGatherer.getMCC
import com.urbandata.pixelsdk.InformationGatherer.getMNC
import com.urbandata.pixelsdk.InformationGatherer.getMaidType
import com.urbandata.pixelsdk.InformationGatherer.getSSID
import com.urbandata.pixelsdk.InformationGatherer.getUserAgent
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
        adTrakingParams.device_id = getHashedAndroidID(ctx)
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
        adTrakingParams.useragent = getUserAgent(ctx)
        adTrakingParams.ipv4 = getIPV4Address()
        adTrakingParams.ipv6 = getIPV6Address()
        adTrakingParams.bssid = getBSSID(ctx)
        adTrakingParams.ssid = getSSID(ctx)
        adTrakingParams.keyboard_language = getKeyboardLanguage(ctx)

        return adTrakingParams
    }
}
