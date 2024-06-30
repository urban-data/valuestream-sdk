package com.yesitlabs.adtraking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yesitlabs.adtraking.InformationGatherer.getAdvertisingId
import com.yesitlabs.adtraking.InformationGatherer.getPostalCodeAndCountry
import com.yesitlabs.adtraking.InformationGatherer.getUserAgent
import com.yesitlabs.adtraking.InformationGatherer.getVendorIdentifier
import com.yesitlabs.adtraking.InformationGatherer.getKeyboardLanguage
import com.yesitlabs.adtraking.InformationGatherer.getAppBundleId
import com.yesitlabs.adtraking.InformationGatherer.getAppName
import com.yesitlabs.adtraking.InformationGatherer.getBSSID
import com.yesitlabs.adtraking.InformationGatherer.getConnectionCountryCode
import com.yesitlabs.adtraking.InformationGatherer.getConnectionProvider
import com.yesitlabs.adtraking.InformationGatherer.getIMEI
import com.yesitlabs.adtraking.InformationGatherer.getSSID
import com.yesitlabs.adtraking.InformationGatherer.getConnectionType
import com.yesitlabs.adtraking.InformationGatherer.getDeviceBrand
import com.yesitlabs.adtraking.InformationGatherer.getDeviceModel
import com.yesitlabs.adtraking.InformationGatherer.getDeviceOSV
import com.yesitlabs.adtraking.InformationGatherer.getDeviceType
import com.yesitlabs.adtraking.InformationGatherer.getIPV4Address
import com.yesitlabs.adtraking.InformationGatherer.getIPV6Address
import com.yesitlabs.adtraking.Utils.alertBoxLocation
import com.yesitlabs.adtraking.Utils.calculateMD5HashEmail
import com.yesitlabs.adtraking.Utils.calculateSHA256Hash
import com.yesitlabs.adtraking.Utils.displayLocationSettingsRequest
import com.yesitlabs.adtraking.Utils.isGPSEnabled
import com.yesitlabs.adtraking.Utils.isLocationPermissionGranted
import com.yesitlabs.adtraking.Utils.isOnline
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response
import java.lang.ref.WeakReference
import java.util.*


class Adtraking(context: Context) : AppCompatActivity() {
    private var startTime: Long = Calendar.getInstance().time.time
    private lateinit var gender: String
    private lateinit var licensekey: String
    private lateinit var yod: String
    private lateinit var email: String
    private val formattedTimestamp = System.currentTimeMillis() / 1000L
    private var contextRef: WeakReference<Context> = WeakReference(context)

    private fun getContext(): Context? {
        val ctx = contextRef.get()
        if (ctx == null) {
            Log.e("Context Error", "Passed context is null!")
        }

        return ctx
    }

    private fun requestLocationPermission() {
        val ctx = getContext() ?: return

        ActivityCompat.requestPermissions(
            ctx as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101
        )
    }


    /**
     * The onRequestPermissionsResult function is called when the user responds to a permission request. It takes two parameters: requestCode (an integer representing the code for the permission request) and grantResults (an array of integers representing the result for each requested permission).
    Within the function:

    1. It checks if the requestCode is 101, indicating that the permission request pertains to location services.

    2. It verifies if grantResults is not empty and whether the first element in grantResults array indicates that the permission was granted.

    Note: There is an error in the permission check logic.
     * It should use grantResults[0] == PackageManager.PERMISSION_GRANTED, but it incorrectly uses grantResults[0] + grantResults[0] == PackageManager.PERMISSION_GRANTED.

    3. If the permission is granted, it calls the displayLocationSettingsRequest(context) method, which presumably initiates a request to display the location settings.

    4. If the permission is not granted, it calls the alertBoxLocation() method, which likely displays an alert box to the user indicating that location permissions are necessary.
     */

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        val ctx = getContext() ?: return
        // Check if the location permission is granted by the user
        if (requestCode == 101 && grantResults.isNotEmpty() && (grantResults[0] + grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            displayLocationSettingsRequest(ctx, onSuccess = { sendDataToServer() })
        } else {
            alertBoxLocation(ctx)
        }
    }


    /**
     *
    The onActivityResult function is called when an activity that was started for a result returns its result. This function takes two parameters: requestCode (an integer identifying the request) and resultCode (an integer representing the result of the activity).
    Within the function:

    1. It first checks if the requestCode is 100, indicating that the result is related to the location permission request.

    2. If the requestCode is 100, it then checks if the resultCode is Activity.RESULT_OK, which indicates that the location permission was successfully granted by the user.

    1. If the resultCode is Activity.RESULT_OK, it calls the apiData() method, which presumably initiates some data fetching or API call that requires location access.
    2. If the resultCode is not Activity.RESULT_OK, it shows a toast message to the user saying "Please turn on location".

    3. If the requestCode is not 100, it calls the displayLocationSettingsRequest(context) method, which likely initiates a request to display the location settings to the user.

     * */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        val ctx = getContext() ?: return
        // Check if the location permission is granted by the user
        if (requestCode == 100) {
            if (Activity.RESULT_OK == resultCode) {
                sendDataToServer()
            } else {
                Toast.makeText(ctx, "Please turn on location", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            displayLocationSettingsRequest(ctx, onSuccess = { sendDataToServer() })
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendDataToServer() {
        val ctx = getContext() ?: return

        GlobalScope.launch(Dispatchers.Main) {
            val horizontalAccuracy: Float
            val verticalAccuracy: Float
            val speed: Float
            val gpsLat: Double
            val gpsLong: Double
            val altitude: Double
            val locationType: String
            val gps = TrackerGps(ctx)
            val gpsLocation = gps.getLocation()
            if (gpsLocation != null) {
                horizontalAccuracy = gpsLocation.accuracy
                verticalAccuracy = gpsLocation.verticalAccuracyMeters
                gpsLat = gpsLocation.latitude
                gpsLong = gpsLocation.longitude
                altitude = gpsLocation.altitude
                locationType = gpsLocation.provider.toString()
                speed = gpsLocation.speedAccuracyMetersPerSecond
                val (postalCode, country) = getPostalCodeAndCountry(
                    ctx,
                    gpsLocation.latitude,
                    gpsLocation.longitude
                )

                val advertisingId = getAdvertisingId(ctx)

                val maididType = "GAID"

                val cellId = getVendorIdentifier(ctx)

                val userAgent = getUserAgent(ctx)

                val language = getKeyboardLanguage(ctx)

                val appBundleId = getAppBundleId(ctx)

                val appName = getAppName(ctx)

                val sSID = getSSID(ctx)

                val bSSID = getBSSID(ctx)

                val imei = getIMEI(ctx)

                val countryCode = getConnectionCountryCode(ctx)

                val connectionProvider = getConnectionProvider(ctx)

                val connectionType = getConnectionType(ctx)

                val deviceType = getDeviceType(ctx)

                val mnc = ctx.resources.configuration.mnc

                val mcc = ctx.resources.configuration.mcc

                val mSISDN = calculateSHA256Hash("Phone")

                val hem = calculateMD5HashEmail(email)

                val currentLocale = Locale.getDefault()

                val languageDisplayName = currentLocale.getDisplayName(currentLocale)

                val sessionTime = Utils.timePassedSince(startTime)

                val apiInterface: Api = RetrofitClient.getClient()!!.create(Api::class.java)

                val deviceBrand = getDeviceBrand()

                val deviceModel = getDeviceModel()

                val deviceBrandAndModel = "$deviceBrand $deviceModel"

                val deviceOSV = getDeviceOSV()

                val call: Call<ApiModel> = apiInterface.addData(
                    licensekey,
                    deviceType,
                    deviceBrandAndModel,
                    gpsLat.toString(),
                    gpsLong.toString(),
                    gender,
                    altitude.toString(),
                    maididType,
                    cellId,
                    userAgent,
                    language,
                    appBundleId,
                    appName,
                    sSID,
                    bSSID,
                    imei,
                    hem,
                    locationType,
                    verticalAccuracy.toString(),
                    horizontalAccuracy.toString(),
                    country,
                    countryCode,
                    connectionProvider,
                    deviceOSV,
                    "android",
                    deviceModel,
                    deviceBrand,
                    connectionType,
                    getIPV4Address(),
                    getIPV6Address(),
                    postalCode,
                    yod,
                    mnc.toString(),
                    mcc.toString(),
                    sessionTime,
                    "",
                    speed.toString(),
                    formattedTimestamp.toString(),
                    mSISDN,
                    advertisingId,
                    languageDisplayName
                )
                call.enqueue(object : retrofit2.Callback<ApiModel> {
                    override fun onResponse(
                        call: Call<ApiModel>,
                        response: Response<ApiModel>
                    ) {
                        try {
                            if (response.body()!!.success) {
                                val sendError = ApiModel(
                                    response.body()!!.license_key,
                                    response.body()!!.message,
                                    response.body()!!.success
                                )
                                Toast.makeText(ctx, sendError.message, Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                val sendError = ApiModel(
                                    license_key = "non",
                                    message = "Something went wrong",
                                    success = false
                                )
                                Toast.makeText(ctx, sendError.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiModel>, t: Throwable) {
                        val sendError = ApiModel(
                            license_key = "non",
                            message = "Something went wrong",
                            success = false
                        )
                        Toast.makeText(ctx, sendError.message, Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(ctx, "Api Not working", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun sendData(
        gender: String,
        licenseKey: String,
        yod: String,
        email: String
    ): Boolean {
        val ctx = getContext() ?: return false

        if (isOnline(ctx)) {
            this.gender = gender
            this.licensekey = licenseKey
            this.yod = yod
            this.email = email
            if (isLocationPermissionGranted(ctx)) {
                if (isGPSEnabled(ctx)) {
                    sendDataToServer()
                } else {
                    displayLocationSettingsRequest(ctx, onSuccess = { sendDataToServer() })
                }
            } else {
                requestLocationPermission()
            }
        } else {
            Toast.makeText(ctx, "Please check your Internet connection", Toast.LENGTH_SHORT)
                .show()
        }

        return true
    }
}

