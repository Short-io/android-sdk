
# ShortIOSDK for Android

This SDK allows you to create short links using the [Short.io](https://short.io/) API based on a public API key and custom parameters. It also supports Android deep linking integration.

## ✨ Features

- Generate short links via Short.io API
- Customize short links using parameters
- Integrate Deeplinking in Android
- Simple and clean API for developers


## 📦 Installation

You can integrate the SDK into your Android Studio project using **JitPack** 

To install the SDK via JitPack:

### 1. Add JitPack Repository

To add the JitPack repository to your build file, Add it in your root `settings.gradle` at the end of repositories:

#### For `settings.gradle`:
```java
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' } // Add this line
	}
}
```

#### For `settings.gradle.kts`
```kotlin
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url = uri("https://jitpack.io") } // Add this line
	}
}
```

### 2. Add the dependency

Open App level build.gradle file `build.gradle.kts (Module:app)`, Add the dependency:


It will be:
```kotlin
dependencies {
	implementation("com.github.User:Repo:Tag") // Example
	implementation("com.github.Short-io:android-sdk:v1.0.5") // Use this
}
```
### 3. Sync the Project
Sync the Project with Gradle file, So the SDK can be Installed.

### 4. Import the SDK
Import the SDK where it is needed by using:
```kotlin
import com.github.shortiosdk.ShortioSdk
``` 

## 🔑 Getting Started

### Step 1: Get Public API Key from Short.io

1. Visit [Short.io](https://short.io/) and **sign up** or **log in** to your account.
   
2. In the dashboard, navigate to **Integrations & API**.

3. Click **CREATE API KEY** button.

4. Enable the **Public Key** toggle.

5. Click **CREATE** to generate your API key.

### 🔗 SDK Usage

```kotlin
import com.github.shortiosdk.ShortioSdk
import com.github.shortiosdk.ShortIOParameters
import com.github.shortiosdk.ShortIOResult

try {
    val params = ShortIOParameters(
      domain = "your_domain", // Replace with your Short.io domain
      originalURL = "your_originalURL" // Replace with your Short.io domain
    )
} catch (e: Exception) {
    Log.e("ShortIO", "Error: ${e.message}", e)
}
```

**Note**: Both `domain` and `originalURL` are the required parameters. You can also pass optional parameters such as `path`, `title`, `utmParameters`, etc.

```kotlin
val apiKey = "your_public_apiKey" // Replace with your Short.io Public API Key

thread {
    try {
        when (val result = ShortioSdk.shortenUrl(apiKey, params)) {
            is ShortIOResult.Success -> {
                Log.d("ShortIOResult","Shortened URL: ${result.data.shortURL}")
            }
            is ShortIOResult.Error -> {
                val error = result.data
                Log.d("ShortIOResult","Error ${error.statusCode}: ${error.message} (code: ${error.code})")
            }
        }
    } catch (e: Exception) {
        Log.e("ShortIO", "Error: ${e.message}", e)
    }
}       
```
## 📄 API Parameters

The `ShortIOParameters` struct is used to define the details of the short link you want to create. Below are the available parameters:


| Parameter           | Type        | Required  | Description                                                  |
| ------------------- | ----------- | --------  | ------------------------------------------------------------ |
| `domain`            | `String`    | ✅        | Your Short.io domain (e.g., `example.short.gy`)              |
| `originalURL`       | `String`    | ✅        | The original URL to be shortened                             |
| `cloaking`          | `Boolean`   | ❌        | If `true`, hides the destination URL from the user           |
| `password`          | `String`    | ❌        | Password to protect the short link                           |
| `redirectType`      | `Int`       | ❌        | Type of redirect (e.g., 301, 302)                            |
| `expiresAt`         | `StringOrInt`| ❌     | Expiration timestamp in Unix format                          |
| `expiredURL`        | `String`    | ❌        | URL to redirect after expiration                             |
| `title`             | `String`    | ❌        | Custom title for the link                                    |
| `tags`              | `[String]`  | ❌        | Tags to categorize the link                                  |
| `utmSource`         | `String`    | ❌        | UTM source parameter                                         |
| `utmMedium`         | `String`    | ❌        | UTM medium parameter                                         |
| `utmCampaign`       | `String`    | ❌        | UTM campaign parameter                                       |
| `utmTerm`           | `String`    | ❌        | UTM term parameter                                           |
| `utmContent`        | `String`    | ❌        | UTM content parameter                                        |
| `ttl`               | `StringOrInt`| ❌        | Time to live for the short link                           |
| `path`              | `String`    | ❌        | Custom path for the short link                               |
| `androidURL`        | `String`    | ❌        | Fallback URL for Android                                     |
| `iphoneURL`         | `String`    | ❌        | Fallback URL for iPhone                                      |
| `createdAt`         | `StringOrInt`| ❌     | Custom creation timestamp. 	                               |
| `clicksLimit`       | `Int`       | ❌        | Maximum number of clicks allowed                             |
| `passwordContact`   | `Boolean`   | ❌        | Whether contact details are required for password access     |
| `skipQS`            | `Boolean`   | ❌        | If `true`, skips query string on redirect (default: `false`) |
| `archived`          | `Boolean`   | ❌        | If `true`, archives the short link (default: `false`)        |
| `splitURL`          | `String`    | ❌        | URL for A/B testing                                          |
| `splitPercent`      | `Int`       | ❌        | Split percentage for A/B testing                             |
| `integrationAdroll` | `String`    | ❌        | AdRoll integration token                                     |
| `integrationFB`     | `String`    | ❌        | Facebook Pixel ID                                            |
| `integrationGA`     | `String`    | ❌        | Google Analytics ID                                          |
| `integrationGTM`    | `String`    | ❌        | Google Tag Manager container ID                              |
| `folderId`          | `String`    | ❌        | ID of the folder where the link should be created            |

## ⚠️ Import And Use of `StringOrInt` Type for Specific Parameters

To Import **`StringOrInt`** type for specific parameters like **expiresAt**, **ttl** and **createdAt**:

```kotlin
import com.github.shortiosdk.StringOrInt
```

And to use it:

```kotlin
val params = ShortIOParametersModel(
    // Example # 01
    expiresAt = StringOrInt.IntVal(1) // OR
    expiresAt = StringOrInt.Str("1")
    // Example # 02
    ttl = StringOrInt.Str("Hello") // OR
    ttl = StringOrInt.IntVal(1)
    // Example # 03
    createdAt = StringOrInt.Str("Hello1234") //OR
    createdAt = StringOrInt.IntVal(1)
)
```

## 🤖 Deep Linking Setup
To handle deep links via Short.io on Android, you'll need to set up Android App Links properly using your domain's Digital Asset Links and intent filters.

### 🔧 Step 1: Configure Intent Filter in AndroidManifest.xml

1. Open your Android project.

2. Navigate to android/app/src/main/AndroidManifest.xml.

3. Inside your MainActivity, add an intent filter to handle app links:
```
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask">
    
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="https"
            android:host="yourshortdomain.short.gy" />
    </intent-filter>
</activity>
```
✅ Tip: Replace yourshortdomain.short.gy with your actual Short.io domain.

### 🛡️ Step 2: Configure Asset Links on Short.io

1. Go to Short.io.

2. Navigate to Domain Settings > Deep links for your selected domain.

3. In the Android Package Name field, enter your app's package name (e.g., com.example.app).

4. In the SHA-256 Certificate Fingerprint field, enter your release key’s SHA-256 fingerprint.
```
// Example Package:
com.example.app

// Example SHA-256:
A1:B2:C3:D4:E5:F6:...:Z9
```
You can retrieve the fingerprint using the following command:

```
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
📌 Note: Use the SHA-256 of your release keystore for production builds.

5. Click Save to update the Digital Asset Links.

### 🚦 Step 3: Enable the Link in "Open by Default"

1. Build and install your app on the device.

2. Go to **App Settings > Open by Default**.

3. Tap on **“Add link”** under the **Open by Default** section.

4. Add your URL and make sure to enable the checkbox for your link.

### 🔗 Step 4: Open the App Using a Deep Link

1. Open a Notes, Email or messaging app on your device.

2. Tap a deep link (e.g., https://yourdomain.com/your-path).

3. If configured properly, your app will appear as an option to handle the link, or it will directly open the app.

### 🧭 Step 5: Handle Incoming URLs with onNewIntent() Method

1. Open your main activity file (e.g., MainActivity.kt).

2. Override the `onNewIntent()` method to receive new intents when the activity is already running:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val result = ShortioSdk.handleIntent(intent)
    Log.d("New Intent", "Host: ${result?.host}, Path: ${result?.path}")
}
```

3. In the same activity, also handle the initial intent inside the `onCreate()` method:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
        val result = ShortioSdk.handleIntent(intent)
    Log.d("New Intent", "Host: ${result?.host}, Path: ${result?.path}")
}
```


### ✅ Final Checklist

* App is signed with the correct keystore.

* The domain is verified on Short.io.
  
* The intent-filter is added in AndroidManifest.xml.
  
* App is installed from Play Store or via direct install (for testing with ADB).

Once these steps are complete, clicking a Short.io link (with your domain) will open the app directly if installed.
