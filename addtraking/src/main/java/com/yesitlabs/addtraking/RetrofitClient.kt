package com.yesitlabs.addtraking

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Request

class RetrofitClient {

    companion object {
        private var retrofit: Retrofit? = null

//        var BASE_URL = "https://adtracking.tgastaging.com/api/"
        var BASE_URL = "https://3.138.201.150/api/"

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

        private val okHttpClient = OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.HOURS)
            .readTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val builder: Request.Builder = chain.request().newBuilder()
                builder.addHeader("X-API-KEY", "YIL_USER_6")
                chain.proceed(builder.build())
            })
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

    }


}