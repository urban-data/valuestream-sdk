package com.yesitlabs.adtraking

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface Api {

    @FormUrlEncoded
    @POST("api/data")
    fun addData(
        // both website and mobile
        @Field("lk") license_key: String?,
        @Field("ts") unix_timestamp: String?,
        @Field("dt") device_type: String?,
        @Field("os") device_os: String?,
        @Field("osv") device_osv: String?,
        @Field("ct") connection_type: String?,
        @Field("cp") connection_provider: String?,
        @Field("cn") country: String?,
        @Field("cc") country_code: String?,
        @Field("v4") ipv4: String?,
        @Field("v6") ipv6: String?,
        @Field("frlat") latitude: String?,
        @Field("frlon") longitude: String?,
        @Field("alt") altitude: String?,
        @Field("lt") location_type: String?,
        @Field("cs") session_duration: String?,
        @Field("frlan") language: String?,
        @Field("ua") useragent: String?,
        // mobile-only
        @Field("maid") maid: String?,
        @Field("maid_type") maid_id: String?,
        @Field("dmh") device_model_hmv: String?,
        @Field("dm") device_model: String?,
        @Field("dbr") device_brand: String?,
        @Field("spd") speed: String?,
        @Field("ha") horizontal_accuracy: String?,
        @Field("va") vertical_accuracy: String?,
        @Field("hem") hem: String?,
        @Field("msisdn") msisdn: String?,
        @Field("imei") imei: String?,
        @Field("bssid") bssid: String?,
        @Field("ssid") ssid: String?,
        @Field("an") app_name: String?,
        @Field("ab") app_bundle: String?,
        @Field("kl") keyboard_language: String?,
        @Field("gnr") gender: String?,
        @Field("yob") yob: String?,
        @Field("cell_id") cell_id: String?,
        @Field("cell_lac") cell_lac: String?,
        @Field("cell_mnc") cell_mnc: String?,
        @Field("cell_mcc") cell_mcc: String?,
    ): Call<ApiModel>
}