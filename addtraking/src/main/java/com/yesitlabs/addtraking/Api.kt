package com.yesitlabs.addtraking

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface Api {

    @FormUrlEncoded
    @POST("add-data")
    fun addData(
        @Field("license_key") license_key: String?,
        @Field("device_type") device_type: String?,
        @Field("device_model") device_model: String?,
        @Field("latitude") latitude: String?,
        @Field("longitude") longitude: String?,
        @Field("gender") gender: String?,
        @Field("altitude") altitude: String?,
        @Field("maid_type") maid_id: String?,
        @Field("cell_id") cell_id: String?,
        @Field("useragent") useragent: String?,
        @Field("keyboard_language") keyboard_language: String?,
        @Field("app_bundle") app_bundle: String?,
        @Field("app_name") app_name: String?,
        @Field("ssid") ssid: String?,
        @Field("bssid") bssid: String?,
        @Field("imei") imei: String?,
        @Field("hem") hem: String?,
        @Field("location_type") location_type: String?,
        @Field("vertical_accuracy") vertical_accuracy: String?,
        @Field("horizontal_accuracy") horizontal_accuracy: String?,
        @Field("country") country: String?,
        @Field("country_code") country_code: String?,
        @Field("connection_provider") connection_provider: String?,
        @Field("device_osv") device_osv: String?,
        @Field("device_os") device_os: String?,
        @Field("device_model_hmv") device_model_hmv: String?,
        @Field("device_brand") device_brand: String?,
        @Field("connection_type") connection_type: String?,
        @Field("ipv4") ipv4: String?,
        @Field("ipv6") ipv6: String?,
        @Field("cell_lac") cell_lac: String?,
        @Field("yob") yob: String?,
        @Field("cell_mnc") cell_mnc: String?,
        @Field("cell_mcc") cell_mcc: String?,
        @Field("session_duration") session_duration: String?,
        @Field("device_browser") device_browser: String?,
        @Field("speed") speed: String?,
        @Field("unix_timestamp") unix_timestamp: String?,
        @Field("msisdn") msisdn: String?,
        @Field("maid") maid: String?,
        @Field("language") language: String?
    ): Call<ApiModel>
}