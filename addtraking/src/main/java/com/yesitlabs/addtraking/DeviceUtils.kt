package com.yesitlabs.addtraking

import android.content.Context
import android.content.res.Configuration

class DeviceUtils {
    companion object{
        fun getDeviceType(context: Context): String? {
            val screenSize: Int = context.getResources()
                .getConfiguration().screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            return when (screenSize) {
                Configuration.SCREENLAYOUT_SIZE_SMALL -> "Small device" // e.g., phone
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> "Normal device" // e.g., phone
                Configuration.SCREENLAYOUT_SIZE_LARGE -> "Large device" // e.g., tablet
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> "XLarge device" // e.g., tablet
                else -> "Unknown device type"
            }
        }
    }
}