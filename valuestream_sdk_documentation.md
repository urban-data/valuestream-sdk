# ValueStream SDK for Android - Integration Guide

## Prerequisites

To integrate our SDK, you will need a **license key** for SDK authentication with our backend.

## Setup & Installation

### Step 1: Add JitPack Repository

In your **settings.gradle**, add JitPack to repositories:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependency

In your **module-level build.gradle**:

```groovy
dependencies {
    implementation 'com.github.urban-data:valuestream-sdk:1.2.2'
}
```

That's it! Sync Gradle and you're ready to go.

---

## Permissions & Data Collection

### Overview

The SDK collects data based on permissions granted by the user. **No data is collected without the corresponding permission.** If a permission is not granted, those fields are simply omitted from data collection.

### Required Permissions in AndroidManifest.xml

Add these permissions to your `AndroidManifest.xml`:

```xml
<!-- Internet (Required) -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Location (Recommended) - Required for GPS data, WiFi SSID/BSSID, and cell tower info -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Phone State (Optional - for device identifiers and cell_id on Android 10+) -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- Phone Number (Optional - rarely granted) -->
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />

<!-- Network State (Recommended) -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Important Notes:**
- **WiFi SSID/BSSID:** Starting with Android 8.1 (API 27), accessing WiFi SSID and BSSID requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`, not just `ACCESS_WIFI_STATE`.
- **Cell Tower ID:** On Android 10+ (API 29), accessing `cell_id` requires both location permissions AND `READ_PHONE_STATE`.
- **Runtime Permissions:** Location and phone state permissions must be requested at runtime on Android 6.0+ (API 23).

### Data Collection by Permission Level

#### **No Permissions Required (25 fields) - Always Collected**

These fields are collected without any runtime permissions:

| Field | Description |
|-------|-------------|
| `device_id` | MD5 hashed Android ID |
| `license_key` | Your SDK license key |
| `connection_provider` | Network operator name (e.g., "Verizon") |
| `unix_timestamp` | Current Unix timestamp |
| `device_type` | "Mobile" or "Tablet" |
| `device_os` | Full OS string (e.g., "Android 33 (13)") |
| `device_osv` | OS version number |
| `connection_type` | "Wi-Fi" or "Mobile Data" |
| `ipv4` | Device IPv4 address |
| `ipv6` | Device IPv6 address |
| `session_duration` | Seconds since app session started |
| `language` | Device locale language |
| `useragent` | WebView user agent string |
| `maid` | Google Advertising ID (GAID) |
| `maid_id` | Always "GAID" |
| `device_model_hmv` | Device hardware identifier |
| `device_model` | Device model (e.g., "Pixel 7") |
| `device_brand` | Device brand (e.g., "Samsung") |
| `hem` | MD5 hashed email (if provided via `setUserDetails()`) |
| `app_name` | Host app display name |
| `app_bundle` | Host app package name |
| `keyboard_language` | Active keyboard language |
| `gender` | User gender (if provided via `setUserDetails()`) |
| `yob` | Year of birth (if provided via `setUserDetails()`) |
| `cell_mnc` | Mobile Network Code |
| `cell_mcc` | Mobile Country Code |

#### **Location Permissions (13 fields)**

Requires: `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`

| Field | Description |
|-------|-------------|
| `latitude` | GPS latitude coordinate |
| `longitude` | GPS longitude coordinate |
| `altitude` | Altitude in meters |
| `horizontalAccuracy` | GPS horizontal accuracy |
| `speed` | Movement speed in m/s |
| `verticalAccuracyMeters` | GPS vertical accuracy |
| `country_code` | 2-letter country code (derived from coordinates) |
| `country` | Full country name (derived from coordinates) |
| `location_type` | Location provider ("gps", "network", "fused") |
| `bssid` | WiFi access point MAC address (requires location on Android 8.1+) |
| `ssid` | WiFi network name (requires location on Android 8.1+) |
| `cell_lac` | Location Area Code (requires `ACCESS_COARSE_LOCATION`) |
| `cell_id` | Cell tower ID (requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`, plus `READ_PHONE_STATE` on Android 10+) |

#### **Phone State Permission (1 field)**

Requires: `READ_PHONE_STATE`

| Field | Description |
|-------|-------------|
| `imei` | MD5 hashed IMEI |

**Note:** `cell_id` is listed under Location Permissions as it primarily requires location permissions, with `READ_PHONE_STATE` additionally required on Android 10+.

#### **Phone Number Permissions (1 field)**

Requires: `READ_PHONE_STATE` + `READ_SMS` + `READ_PHONE_NUMBERS`

| Field | Description |
|-------|-------------|
| `msisdn` | MD5 hashed phone number |


### Where to Initialize

Initialize the SDK **once** in your `Application` class's `onCreate()` method. This ensures the SDK starts when your app launches.

#### Example Application Class

**Kotlin:**
```kotlin
import android.app.Application
import com.urbandata.pixelsdk.PixelSDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize PixelSDK
        val licenseKey = BuildConfig.PIXEL_SDK_LICENSE_KEY
        val intervalInMinutes: Long = 5

        PixelSDK.initialize(this, licenseKey, intervalInMinutes)
    }
}
```

**Java:**
```java
import android.app.Application;
import com.urbandata.pixelsdk.PixelSDK;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize PixelSDK
        String licenseKey = BuildConfig.PIXEL_SDK_LICENSE_KEY;
        long intervalInMinutes = 5L;

        PixelSDK.INSTANCE.initialize(this, licenseKey, intervalInMinutes);
    }
}
```

### Secure License Key Management

**Never commit your license key to version control.** Use one of these approaches:

#### Option 1: BuildConfig (Recommended)

In your `local.properties` file (already in `.gitignore`):
```properties
pixelsdk.license.key=YOUR_LICENSE_KEY_HERE
```

In your module-level `build.gradle`:
```groovy
android {
    defaultConfig {
        // Read from local.properties
        Properties properties = new Properties()
        properties.load(project.rootProject.file('local.properties').newDataInputStream())

        buildConfigField "String", "PIXEL_SDK_LICENSE_KEY",
            "\"${properties.getProperty('pixelsdk.license.key', '')}\""
    }
}
```

Then use: `BuildConfig.PIXEL_SDK_LICENSE_KEY`

#### Option 2: Environment Variables

```groovy
android {
    defaultConfig {
        buildConfigField "String", "PIXEL_SDK_LICENSE_KEY",
            "\"${System.getenv('PIXEL_SDK_LICENSE_KEY') ?: ''}\""
    }
}
```

### Data Collection Behavior

- **Collection Interval:** Data is collected at the interval you specify (default: 5 minutes)
- **Foreground Only:** Data collection **stops** when the app goes to the background
- **Permission-Based:** Only data with granted permissions is collected
- **Singleton Pattern:** One instance across your entire app

**Important:** The SDK does **not** run in the background. When users exit your app, data collection pauses automatically.


## User Details

You can optionally provide additional user information that will be included with collected data. This is useful for user segmentation and analytics.

### Setting User Details

Call `setUserDetails()` anywhere in your app after initialization:

**Kotlin:**
```kotlin
import com.urbandata.pixelsdk.PixelSDK

// Set user details
val email = "user@example.com"
val yearOfBirth = "1990"
val gender = "male"  // or "female", or any custom value

PixelSDK.setUserDetails(email, yearOfBirth, gender)
```

**Java:**
```java
import com.urbandata.pixelsdk.PixelSDK;

// Set user details
String email = "user@example.com";
String yearOfBirth = "1990";
String gender = "male";  // or "female", or any custom value

PixelSDK.INSTANCE.setUserDetails(email, yearOfBirth, gender);
```

### What Happens to User Details?

- **Email:** Hashed with MD5 before transmission (stored as `hem` field)
- **Year of Birth:** Sent as-is (stored as `yob` field)
- **Gender:** Sent as-is (stored as `gender` field)
- **Persistence:** Once set, these details are included in all subsequent data transmissions until changed
