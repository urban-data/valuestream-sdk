package com.yesitlabs.adtraking

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient {

    companion object {

        private var retrofit: Retrofit? = null

//        private var BASE_URL = "https://adtracking.tgastaging.com/api/"
        private var BASE_URL = "https://3.138.201.150/api/"

        fun getClient(): Retrofit? {
            val gson = GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss")
                .create()
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            }
            return retrofit
        }

        private val okHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(1, TimeUnit.MINUTES) // connect timeout
            .writeTimeout(1, TimeUnit.MINUTES) // write timeout
            .readTimeout(1, TimeUnit.MINUTES) // read timeout
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

    }


}