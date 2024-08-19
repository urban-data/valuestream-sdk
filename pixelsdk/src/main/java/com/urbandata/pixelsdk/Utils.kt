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
import java.util.Locale

object Utils {
    var SDK_LOG_TAG: String = "PixelSDK"

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
        val currentTime = System.currentTimeMillis()
        val secondsPassed = (currentTime - time) / 1000
        return secondsPassed.toString()
    }

    fun getCurrentUnixTimestamp(): String {
        return (System.currentTimeMillis() / 1000L).toString()
    }

    fun logError(msg: String) {
        Log.w(SDK_LOG_TAG, msg);
    }

    fun logInfo(msg: String) {
        Log.i(SDK_LOG_TAG, msg);
    }

    suspend fun getStaticData(ctx: Context, pixelSDKParams: PixelSDKParams): PixelSDKParams {
        pixelSDKParams.device_id = getHashedAndroidID(ctx)
        pixelSDKParams.device_type = getDeviceType(ctx)
        pixelSDKParams.device_os = getDeviceOS()
        pixelSDKParams.device_osv = getDeviceOSV()
        pixelSDKParams.device_brand = getDeviceBrand() ?: ""
        pixelSDKParams.device_model = getDeviceModel() ?: ""
        pixelSDKParams.device_model_hmv = getDeviceHardware() ?: ""
        val location = TrackerGps.getLocation()

        if (location != null) {
            val (country, countryCode) = getCountry(ctx, location.latitude, location.longitude)
            pixelSDKParams.latitude = location.latitude.toString()
            pixelSDKParams.longitude = location.longitude.toString()
            pixelSDKParams.country = country
            pixelSDKParams.country_code = countryCode
        }

        pixelSDKParams.maid = getMAID(ctx)
        pixelSDKParams.maid_id = getMaidType()
        pixelSDKParams.msisdn = getHashedMSISDN(ctx)
        pixelSDKParams.imei = getHashedIMEI(ctx)
        pixelSDKParams.app_name = getAppName(ctx)
        pixelSDKParams.app_bundle = getAppBundleId(ctx)
        pixelSDKParams.cell_id = getCellId(ctx)
        pixelSDKParams.cell_lac = getCellLac(ctx)
        pixelSDKParams.cell_mnc = getMNC(ctx)
        pixelSDKParams.cell_mcc = getMCC(ctx)
        return pixelSDKParams
    }

    suspend fun getDynamicData(
        ctx: Context,
        startTime: Long,
        pixelSDKParams: PixelSDKParams
    ): PixelSDKParams {
        pixelSDKParams.unix_timestamp = getCurrentUnixTimestamp()
        pixelSDKParams.connection_type = getConnectionType(ctx)
        pixelSDKParams.connection_provider = getConnectionProvider(ctx)
        val location = TrackerGps.getLocation()
        if (location != null) {
            pixelSDKParams.latitude = location.latitude.toString()
            pixelSDKParams.longitude = location.longitude.toString()
            pixelSDKParams.altitude = location.altitude.toString()
            pixelSDKParams.bearing = getBearingTo(ctx, location.latitude, location.longitude).toString()
            pixelSDKParams.location_type = location.provider ?: ""
            pixelSDKParams.speed = location.speed.toString()
            pixelSDKParams.horizontalAccuracy = location.accuracy.toString()
            pixelSDKParams.verticalAccuracyMeters = location.verticalAccuracyMeters.toString()
        }

        pixelSDKParams.session_duration = timePassedSince(startTime)
        pixelSDKParams.language = getCurrentLocaleLanguage()
        pixelSDKParams.useragent = getUserAgent(ctx)
        pixelSDKParams.ipv4 = getIPV4Address()
        pixelSDKParams.ipv6 = getIPV6Address()
        pixelSDKParams.bssid = getBSSID(ctx)
        pixelSDKParams.ssid = getSSID(ctx)
        pixelSDKParams.keyboard_language = getKeyboardLanguage(ctx)

        return pixelSDKParams
    }
}
