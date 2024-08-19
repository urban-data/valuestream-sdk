package com.urbandata.pixelsdk

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.urbandata.pixelsdk.Utils.getDynamicData
import com.urbandata.pixelsdk.Utils.getStaticData
import com.urbandata.pixelsdk.Utils.logError
import com.urbandata.pixelsdk.Utils.logInfo
import com.urbandata.pixelsdk.Utils.md5Hash
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates

// maybe make this a non-singleton later if need be
object PixelSDK {
    private var pixelSDKParams: PixelSDKParams = PixelSDKParams()
    private var startTime by Delegates.notNull<Long>()
    private lateinit var contextRef: WeakReference<Context>
    private var sendDataJob: Job? = null
    private var _intervalMinutes : Long = 5L

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        logError("Coroutine exception: ${exception.localizedMessage}")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    fun initialize(ctx: Context, licenseKey: String, intervalMinutes: Long) {
        contextRef = WeakReference(ctx)
        pixelSDKParams.license_key = licenseKey
        _intervalMinutes = intervalMinutes
        TrackerGps.initialize(ctx)

        coroutineScope.launch {
            pixelSDKParams = getStaticData(ctx, pixelSDKParams)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    private fun startSendingData() {
        startTime = Calendar.getInstance().time.time
        stopSendingData() // stop existing tasks
        sendDataJob = coroutineScope.launch {
            while (isActive) {
                sendData()
                delay(_intervalMinutes * 60 * 1000) // Delay for the specified interval in minutes
            }
        }
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

    private suspend fun sendData() {
        val ctx = getContext() ?: return

        val retrofitClient = RetrofitClient.getClient()
        if (retrofitClient == null) {
            logError("RetrofitClient.getClient() is null!")
            return
        }

        pixelSDKParams = getDynamicData(ctx, startTime, pixelSDKParams)
        val call: Call<PixelSDKResponse> =
            retrofitClient.create(Api::class.java).addData(mapPixelSDKParams(pixelSDKParams))

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
            // App has come to the foreground
            startSendingData();
        }

        override fun onStop(owner: LifecycleOwner) {
            // App is going to the background
            stopSendingData()
        }
    }
}

