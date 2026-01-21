package com.urbandata.pixelsdk

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.urbandata.pixelsdk.Utils.collectData
import com.urbandata.pixelsdk.Utils.logError
import com.urbandata.pixelsdk.Utils.logInfo
import com.urbandata.pixelsdk.Utils.md5Hash
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates

object PixelSDK {
    private var pixelSDKParams: PixelSDKParams = PixelSDKParams()
    private var startTime by Delegates.notNull<Long>()
    private lateinit var contextRef: WeakReference<Context>
    private var sendDataJob: Job? = null
    private var _intervalMinutes : Long = 5L

    private val ALLOWED_COUNTRIES = setOf(
        // Middle East
        "AE", // UAE
        "BH", // Bahrain
        "EG", // Egypt
        "IQ", // Iraq
        "IR", // Iran
        "IL", // Israel
        "JO", // Jordan
        "KW", // Kuwait
        "LB", // Lebanon
        "OM", // Oman
        "PS", // Palestine
        "QA", // Qatar
        "SA", // Saudi Arabia
        "SY", // Syria
        "TR", // Turkey
        "YE", // Yemen
        // North Africa
        "DZ", // Algeria
        "LY", // Libya
        "MA", // Morocco
        "MR", // Mauritania
        "SD", // Sudan
        "SS", // South Sudan
        "TN"  // Tunisia
    )

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        logError("Coroutine exception: ${exception.localizedMessage}")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    fun initialize(ctx: Context, licenseKey: String, intervalMinutes: Long, debugMode: Boolean = false) {
        Utils.debugMode = debugMode
        logInfo("initialize: start")
        contextRef = WeakReference(ctx)
        pixelSDKParams.license_key = licenseKey
        _intervalMinutes = intervalMinutes
        startTime = Calendar.getInstance().time.time
        TrackerGps.initialize(ctx)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        logInfo("initialize: end")
    }

    private fun startSendingData() {
        logInfo("startSendingData: start")
        startTime = Calendar.getInstance().time.time
        stopSendingData() // stop existing tasks
        sendDataJob = coroutineScope.launch {
            while (isActive) {
                logInfo("startSendingData: loop iteration, calling sendData")
                sendData()
                logInfo("startSendingData: sendData done, delaying ${_intervalMinutes} min")
                delay(_intervalMinutes * 60 * 1000) // Delay for the specified interval in minutes
            }
        }
        logInfo("startSendingData: end")
    }

    fun stopSendingData() {
        sendDataJob?.cancel()
    }

    fun setUserDetails(email: String, yod: String, gender: String) {
        pixelSDKParams.hem = md5Hash(email)
        pixelSDKParams.yob = yod
        pixelSDKParams.gender = gender
    }

    private fun getContext(): Context? {
        val ctx = contextRef.get()
        if (ctx == null) {
            logError("Passed context is null!")
        }

        return ctx
    }

    private suspend fun fetchIpData(retrofitClient: retrofit2.Retrofit): IpDataResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val call = retrofitClient.create(Api::class.java).getIpData(pixelSDKParams.license_key)
                val response = call.execute()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    logError("fetchIpData: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                logError("fetchIpData error: ${e.message}")
                null
            }
        }
    }

    private suspend fun sendData() {
        logInfo("sendData: start")
        val ctx = getContext() ?: return

        val retrofitClient = RetrofitClient.getClient()
        if (retrofitClient == null) {
            logError("RetrofitClient.getClient() is null!")
            return
        }

        // First, fetch IP data from backend to get country code for filtering
        logInfo("sendData: fetching IP data from backend")
        val ipDataResponse = fetchIpData(retrofitClient)
        if (ipDataResponse == null) {
            logError("sendData: failed to fetch IP data, skipping")
            return
        }

        val countryCode = ipDataResponse.cc ?: ""
        if (countryCode !in ALLOWED_COUNTRIES) {
            logInfo("sendData: country '$countryCode' not in allowed list, skipping")
            return
        }

        logInfo("sendData: country '$countryCode' allowed, collecting data")
        pixelSDKParams = collectData(ctx, startTime, pixelSDKParams)

        // Use IP data from backend response
        pixelSDKParams.ipv4 = ipDataResponse.ip ?: ""
        pixelSDKParams.country_code = countryCode
        pixelSDKParams.connection_provider = ipDataResponse.cp ?: ""

        // Fallback to IP-based location if GPS location not available
        if (pixelSDKParams.latitude.isEmpty() && ipDataResponse.lat != null && ipDataResponse.lon != null) {
            logInfo("sendData: GPS location not available, using IP-based location")
            pixelSDKParams.latitude = ipDataResponse.lat.toString()
            pixelSDKParams.longitude = ipDataResponse.lon.toString()
            pixelSDKParams.location_type = "ip"
        }

        logInfo("sendData: collectData done")
        logInfo("sendData: sending request")
        val requestBody = mapPixelSDKParams(pixelSDKParams)
        logInfo("sendData: request body = $requestBody")
        val call: Call<PixelSDKResponse> =
            retrofitClient.create(Api::class.java).addData(requestBody)

        call.enqueue(object : retrofit2.Callback<PixelSDKResponse> {
            override fun onResponse(
                call: Call<PixelSDKResponse>,
                response: Response<PixelSDKResponse>
            ) {
                try {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        logInfo("Successfully sent the data! " + responseBody.message);
                    } else {
                        logError("An error occurred. " + (responseBody?.message ?: ""));
                    }
                } catch (e: Exception) {
                    logError("An error occurred. " + (e.message ?: ""));
                }
            }

            override fun onFailure(call: Call<PixelSDKResponse>, t: Throwable) {
                logError("An error occurred. " + (t.message ?: ""));
            }
        })
    }


    private class AppLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            logInfo("AppLifecycleObserver: onStart (app to foreground)")
            startSendingData();
        }

        override fun onStop(owner: LifecycleOwner) {
            logInfo("AppLifecycleObserver: onStop (app to background)")
            stopSendingData()
        }
    }
}

