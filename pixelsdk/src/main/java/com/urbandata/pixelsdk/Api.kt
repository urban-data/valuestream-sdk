package com.urbandata.pixelsdk

import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class IpDataResponse(
    val ip: String?,
    val cp: String?,
    val cc: String?,
    val lat: Double?,
    val lon: Double?
)

data class PixelSDKResponse(
    var license_key: String,
    var message: String,
    var success: Boolean
)

data class PixelSDKParams(
    var device_id: String = "",
    var license_key: String = "",
    var latitude: String = "",
    var longitude: String = "",
    var altitude: String = "",
    var connection_provider: String = "",
    var horizontalAccuracy: String = "",
    var speed: String = "",
    var verticalAccuracyMeters: String = "",
    var country_code: String = "",
    var country: String = "",
    var unix_timestamp: String = "",
    var location_timestamp: String = "",
    var device_type: String = "",
    var device_os: String = "",
    var device_osv: String = "",
    var connection_type: String = "",
    var ipv4: String = "",
    var ipv6: String = "",
    var location_type: String = "",
    var session_duration: String = "",
    var language: String = "",
    var useragent: String = "",
    var maid: String = "",
    var maid_id: String = "",
    var device_model_hmv: String = "",
    var device_model: String = "",
    var device_brand: String = "",
    var hem: String = "",
    var msisdn: String = "",
    var imei: String = "",
    var bssid: String = "",
    var ssid: String = "",
    var app_name: String = "",
    var app_bundle: String = "",
    var keyboard_language: String = "",
    var gender: String = "",
    var yob: String = "",
    var cell_id: String = "",
    var cell_lac: String = "",
    var cell_mnc: String = "",
    var cell_mcc: String = ""
)

fun mapPixelSDKParams(params: PixelSDKParams): Map<String, String?> {
    return mapOf(
        "lk" to params.license_key,
        "ts" to params.unix_timestamp,
        "loc_ts" to params.location_timestamp,
        "dt" to params.device_type,
        "os" to params.device_os,
        "osv" to params.device_osv,
        "ct" to params.connection_type,
        "cp" to params.connection_provider,
        "cn" to params.country,
        "cc" to params.country_code,
        "v4" to params.ipv4,
        "v6" to params.ipv6,
        "lat" to params.latitude,
        "lon" to params.longitude,
        "alt" to params.altitude,
        "lt" to params.location_type,
        "cs" to params.session_duration,
        "lang" to (params.language.ifEmpty { params.keyboard_language }),
        "maid" to params.maid,
        "maid_type" to params.maid_id,
        "dmh" to params.device_model_hmv,
        "dm" to params.device_model,
        "dbr" to params.device_brand,
        "spd" to params.speed,
        "ha" to params.horizontalAccuracy,
        "va" to params.verticalAccuracyMeters,
        "hem" to params.hem,
        "msisdn" to params.msisdn,
        "imei" to params.imei,
        "bssid" to params.bssid,
        "ssid" to params.ssid,
        "an" to params.app_name,
        "ab" to params.app_bundle,
        "gnr" to params.gender,
        "yob" to params.yob,
        "cell_id" to params.cell_id,
        "cell_lac" to params.cell_lac,
        "cell_mnc" to params.cell_mnc,
        "cell_mcc" to params.cell_mcc
    )
}


interface Api {
    @GET("api/ipdata")
    fun getIpData(
        @Query("lk") licenseKey: String
    ): Call<IpDataResponse>

    @FormUrlEncoded
    @POST("api/mobile_data")
    fun addData(
        @FieldMap params: Map<String, String?>
    ): Call<PixelSDKResponse>
}