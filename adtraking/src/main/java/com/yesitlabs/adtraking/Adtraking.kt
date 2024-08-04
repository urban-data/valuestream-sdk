package com.urbandata.pixelsdk

import android.content.Context
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
object AdTraking {
    private var adTrakingParams: AdTrakingParams = AdTrakingParams()
    private var startTime by Delegates.notNull<Long>()
    private lateinit var contextRef: WeakReference<Context>
    private var sendDataJob: Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        logError("Coroutine exception: ${exception.localizedMessage}")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    // can be called more than once, no bug will occur, it will just re-initialize
    fun initialize(ctx: Context, licenseKey: String, intervalMinutes: Long) {
        contextRef = WeakReference(ctx)
        adTrakingParams.license_key = licenseKey
        startTime = Calendar.getInstance().time.time
        TrackerGps.initialize(ctx)

        coroutineScope.launch {
            adTrakingParams = getStaticData(ctx, adTrakingParams)

            startSendDataTask(intervalMinutes)
        }
    }

    private fun startSendDataTask(intervalMinutes: Long) {
        sendDataJob?.cancel() // Cancel any existing job
        sendDataJob = coroutineScope.launch {
            while (isActive) {
                sendData()
                delay(intervalMinutes * 60 * 1000) // Delay for the specified interval in minutes
            }
        }
    }

    fun setUserDetails(email: String, yod: String, gender: String) {
        adTrakingParams.hem = md5Hash(email)
        adTrakingParams.yob = yod
        adTrakingParams.gender = gender
    }

    private fun getContext(): Context? {
        val ctx = contextRef.get()
        if (ctx == null) {
            logError("Passed context is null!")
        }

        return ctx
    }

    fun sendData() {
        val ctx = getContext() ?: return

        coroutineScope.launch {
            val retrofitClient = RetrofitClient.getClient()
            if (retrofitClient == null) {
                logError("RetrofitClient.getClient() is null!")
                return@launch
            }

            adTrakingParams = getDynamicData(ctx, startTime, adTrakingParams)
            val call: Call<AdTrakingResponse> =
                retrofitClient.create(Api::class.java).addData(mapAdTrakingParams(adTrakingParams))

            call.enqueue(object : retrofit2.Callback<AdTrakingResponse> {
                override fun onResponse(
                    call: Call<AdTrakingResponse>,
                    response: Response<AdTrakingResponse>
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

                override fun onFailure(call: Call<AdTrakingResponse>, t: Throwable) {
                    logError("An error occurred. " + (t.message ?: ""));
                }
            })
        }
    }

    fun stopSendingData() {
        sendDataJob?.cancel() // Cancel the recurring task if needed
    }
}

