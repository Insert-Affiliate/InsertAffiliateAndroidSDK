# Insert Affiliate Android SDK

The **InsertAffiliateAndroid SDK** is designed for Android applications, providing seamless integration with the [Insert Affiliate platform](https://insertaffiliate.com). This Insert enables functionalities such as managing affiliate links, handling in-app purchases (IAP), and utilising deep links. For more details and to access the Insert Affiliate dashboard, visit [app.insertaffiliate.com](https://app.insertaffiliate.com).

## Features

- **Unique Device ID**: Creates a unique ID to anonymously associate purchases with users for tracking purposes.
- **Affiliate Identifier Management**: Set and retrieve the affiliate identifier based on user-specific links.
- **In-App Purchase (IAP) Initialisation**: Easily reinitialise in-app purchases with the option to validate using an affiliate identifier.

## Getting Started
To get started with the Insert Affiliate Android SDK:

1. [Install the SDK](#installation)
2. [Initialise the SDK inside of MainActivity](#basic-usage)
3. [Set up in-app purchases (required)](#in-app-purchase-setup-required)
4. [Set up deep linking (Required)](#deep-link-setup-required)
5. [Use additional features like event tracking based on your app's requirements.](#additional-features)

## Installation

Follow the steps below to install the SDK. You can use different methods depending on your project setup (e.g., Gradle, Maven, or manual download).

#### Step 1: Add the JitPack repository

In your **root** `build.gradle`, add the JitPack repository to the `repositories` section:

```java
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### Step 2. Add the SDK dependency

In your **module's** `build.gradle`, add the SDK dependency:

```java
dependencies {
    implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.1.4'
}
```

## Basic Usage
### Initialise the InsertAffiliateManager in `MainActivity`

```java
public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    InsertAffiliateManager insertAffiliateManager;
    private ActivityMainBinding binding;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialise InsertAffiliateManager in the main activity
        insertAffiliateManager = new InsertAffiliateManager(MainActivity.this);
        insertAffiliateManager.init(MainActivity.this, "{{ your_company_code }}");
}
```
- Replace `{{ your_company_code }}` with the unique company code associated with your Insert Affiliate account. You can find this code in your dashboard under [Settings](http://app.insertaffiliate.com/settings).

### Advanced Initialization Options

The SDK supports additional initialization parameters for enhanced functionality:

```java
// Basic initialization with optional parameters
InsertAffiliateManager.init(
    activity,                    // Your activity context
    "{{ your_company_code }}",   // Your company code
    true,                        // Enable verbose logging (optional)
    true                         // Enable insert links (includes install referrer)
);

// With timeout only
InsertAffiliateManager.init(
    activity,                    // Your activity context
    "{{ your_company_code }}",   // Your company code
    604800                       // Affiliate attribution timeout in seconds (7 days)
);

// With verbose logging and timeout (common for development)
InsertAffiliateManager.init(
    activity,                    // Your activity context
    "{{ your_company_code }}",   // Your company code
    true,                        // Enable verbose logging
    604800                       // Affiliate attribution timeout in seconds (7 days)
);

// Full initialization with all options
InsertAffiliateManager.init(
    activity,                    // Your activity context
    "{{ your_company_code }}",   // Your company code
    true,                        // Enable verbose logging (optional)
    true,                        // Enable insert links (includes install referrer)
    604800                       // OPTIONAL: Affiliate attribution timeout in seconds (7 days)
);
```

**Parameters:**
- `enableVerboseLogging`: When `true`, provides detailed logging for debugging and should be disabled in production
- `enableInsertLinks`: When `true`, additional logging will occur to handle Insert Links
- `affiliateAttributionActiveTimeSeconds`: **OPTIONAL** - Time in seconds for affiliate attribution to remain active (0 = no timeout)

**Default Behavior:**
- Insert Links and Verbose Logging is disabled by default
- Affiliate attribution timeout is disabled by default (no expiration)


## In-App Purchase Setup [Required]
Insert Affiliate requires a Receipt Verification platform to validate in-app purchases. You must choose **one** of our supported partners:
- [RevenueCat](https://www.revenuecat.com/)
- [Iaptic](https://www.iaptic.com/account)
- [Direct Google Play Integration through RTDN](https://docs.insertaffiliate.com/direct-google-play-store-purchase-integration)

### Option 1: RevenueCat Integration
#### 1. Code Setup
First, complete the [RevenueCat SDK installation](https://www.revenuecat.com/docs/getting-started/installation/android) and configure. Then modify your `MainActivity.java`:



```java
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.LogInCallback;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    InsertAffiliateManager insertAffiliateManager;
    private ActivityMainBinding binding;

    protected void onCreate(Bundle savedInstanceState) {
        InsertAffiliateManager.init(MainActivity.this, {{your_company_code}});

        String insertAffiliateIdentifier = InsertAffiliateManager.returnInsertAffiliateIdentifier(MainActivity.this);
        if(!insertAffiliateIdentifier.equals(null)) {
            Purchases.sharedInstance.setAttributes(mapOf("insert_affiliate" to insertAffiliateIdentifier));
        }
    }
}
```
- Replace `{{ your_company_code }}` with the unique company code associated with your Insert Affiliate account. You can find this code in your dashboard under [Settings](http://app.insertaffiliate.com/settings).

#### 2. Webhook Setup

1. Go to RevenueCat and [create a new webhook](https://www.revenuecat.com/docs/integrations/webhooks)

2. Configure the webhook with these settings:
   - Webhook URL: `https://api.insertaffiliate.com/v1/api/revenuecat-webhook`
   - Authorization header: Use the value from your Insert Affiliate dashboard (you'll get this in step 4)
   - Set "Event Type" to "All events"

3. In your [Insert Affiliate dashboard settings](https://app.insertaffiliate.com/settings):
   - Navigate to the verification settings
   - Set the in-app purchase verification method to `RevenueCat`

4. Back in your Insert Affiliate dashboard:
   - Locate the `RevenueCat Webhook Authentication Header` value
   - Copy this value
   - Paste it as the Authorization header value in your RevenueCat webhook configuration


### Option 2: Iaptic Integration
#### 1. Code Setup
#### Step 1: Set up your in app purchases
In this example we are using the [Google In-App billing Library](https://github.com/moisoni97/google-inapp-billing) - but you can use your favourite.

#### Step 2: Verify your purchase with Iaptic via our SDK
After a user makes a successful purchase, you need to verify and acknowledge the purchase. Add this code in your `InAppFragment` to handle the purchase flow and validate it through `InsertAffiliateManager`.

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;
public class InAppFragment extends Fragment {
    InsertAffiliateManager insertAffiliateManager;
    
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        insertAffiliateManager = new InsertAffiliateManager(getActivity());
    }

    @Override
    public void onProductsPurchased(@NonNull List<games.moisoni.google_iab.models.PurchaseInfo> purchases) {
        for (games.moisoni.google_iab.models.PurchaseInfo purchaseInfo
                : purchases) {
            String orderId = purchaseInfo.getOrderId();
            purchasedInfoList.add(purchaseInfo);

            //Purchase was successful
            insertAffiliateManager.validatePurchaseWithIapticAPI(
                getActivity(),
                "{{ your_iaptic_app_name }}",
                "{{ your_iaptic_public_key }}",
                purchaseInfo.getProduct(),
                orderId,
                purchaseInfo.getPurchaseToken(),
                purchaseInfo.getOriginalJson(),
                purchaseInfo.getSignature()
            );
        }
    }
}
```
- Replace `{{ your_iaptic_app_name }}` with your **Iaptic App Name**. You can find this [here](https://www.iaptic.com/account).
- Replace `{{ your_iaptic_public_key }}` with your **Iaptic Public Key**. You can find this [here](https://www.iaptic.com/settings).

#### 2. Webhook Setup

1. Open the [Insert Affiliate settings](https://app.insertaffiliate.com/settings):
  - Navigate to the Verification Settings section
  - Set the In-App Purchase Verification method to `Iaptic`
  - Copy the `Iaptic Webhook URL` and the `Iaptic Webhook Sandbox URL`- you'll need it in the next step.
2. Go to the [Iaptic Settings](https://www.iaptic.com/settings)
- Paste the copied `Iaptic Webhook URL` into the `Webhook URL` field
- Paste the copied `Iaptic Webhook Sandbox URL` into the `Sandbox Webhook URL` field
- Click **Save Settings**.
3. Check that you have completed the [Iaptic setup for the Google Play Notifications URL](https://www.iaptic.com/documentation/setup/connect-with-google-publisher-api)

### Option 3: Google Play Store Direct Integration
Our direct Google Play Store integration is currently in beta.

#### 1. Real Time Developer Notifications (RTDN) Setup

Visit [our docs](https://docs.insertaffiliate.com/direct-google-play-store-purchase-integration) and complete the required set up steps for Google Play's Real Time Developer Notifications.

#### 2. Implementing Purchases

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class InAppFragment extends Fragment {
    InsertAffiliateManager insertAffiliateManager;

    @Override
    public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
        for (PurchaseInfo purchase : purchases) {
            purchasedInfoList.add(purchase);
            InsertAffiliateManager.storeExpectedPlayStoreTransaction(getActivity(), purchase.getPurchaseToken());
        }
    }
}
```

## Deep Link Setup [Required]
Insert Affiliate requires a Deep Linking platform to create links for your affiliates. Our platform works with **any** deep linking provider, and you only need to follow these steps:
1. **Create a deep link** in your chosen third-party platform and pass it to our dashboard when an affiliate signs up. 
2. **Handle deep link clicks** in your app by passing the clicked link:

```java
insertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, "" + 
{{ referring_link }};
```

### Deep Linking with Insert Links

Insert Links by Insert Affiliate supports direct deep linking into your app. This allows you to track affiliate attribution when end users are referred to your app by clicking on one of your affiliates Insert Links.

#### Initial Setup

1. Before you can use Insert Links, you must complete the setup steps in [our docs](https://docs.insertaffiliate.com/insert-links)

You must enable *insertLinksEnabled* when [initialising our SDK](https://github.com/Insert-Affiliate/InsertAffiliateAndroidSDK/tree/feature/deeplink?tab=readme-ov-file#advanced-initialization-options)

2. **Callback and Insert Links Handling**
All steps below are required for Insert Links to function correctly.

```java
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    InsertAffiliateManager insertAffiliateManager;
    private ActivityMainBinding binding;
    
    @Override
    protected void onCreate() {
        super.onCreate(savedInstanceState);

        // Initialize the SDK
        InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", true, true);

        // 1. Set up callback to be notified when affiliate identifier changes
        InsertAffiliateManager.setInsertAffiliateIdentifierChangeCallback(new InsertAffiliateManager.InsertAffiliateIdentifierChangeCallback() {
            @Override
            public void onIdentifierChanged(String identifier) {
                // Log the new identifier to console
                Log.i("InsertAffiliate TAG", "[Insert Affiliate] Affiliate identifier changed to: " + identifier);

                // *** Required if using RevenueCat *** //
                Purchases.sharedInstance.setAttributes(mapOf("insert_affiliate" to insertAffiliateIdentifier));
                // *** End of RevenueCat section *** //
            }
        });

        // 2. Handle deep link from onCreate
        InsertAffiliateManager.handleInsertLink(this, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // 3. Handle deep link from onNewIntent
        InsertAffiliateManager.handleInsertLink(this, intent);
    }
}
```

### Deep Linking with Branch.io
To set up deep linking with Branch.io, follow these steps:

1. Create a deep link in Branch and pass it to our dashboard when an affiliate signs up.
    - Example: [Create Affiliate](https://docs.insertaffiliate.com/create-affiliate).
2. Modify Your Deep Link Handling in `MainActivity.java`
    - After setting up your Branch integration, add the following code to initialise the Insert Affiliate SDK in your app:

#### Example with RevenueCat
```java
public class MainActivity extends AppCompatActivity {
    InsertAffiliateManager insertAffiliateManager;
    private ActivityMainBinding binding;
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Step 1: Create a callback for Branch when a link is clicked
        Branch.sessionBuilder(this).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null && branchUniversalObject != null) {
                    try {
                        // Step 2: Call the Insert Affiliate SDK with the context and referring link
                        InsertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, "" + branchUniversalObject.getContentMetadata().convertToJson().get("~referring_link"));
                        
                        String insertAffiliateIdentifier = InsertAffiliateManager.returnInsertAffiliateIdentifier(MainActivity.this);
                        Purchases.sharedInstance.setAttributes(mapOf("insert_affiliate" to insertAffiliateIdentifier));
                    }
                }
            }
        }
    }
}
```

#### Example with Iaptic / Direct Google Play Store Integration through RTDN

**1. Modify Branch.io's onStart() to Pass the Referring Link to the Insert Affiliate SDK**

In your `MainActivity.java`, start a Branch.io session when the app is opened, and pass the user's unique ID for tracking. Add the following code in the `onStart()` method:

```java
public class MainActivity extends AppCompatActivity {
    InsertAffiliateManager insertAffiliateManager;
    private ActivityMainBinding binding;
    
    @Override
    protected void onStart() {
        super.onStart();

        // Step 1: Create a callback for Branch when a link is clicked
        Branch.sessionBuilder(this).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null && branchUniversalObject != null) {
                    try {
                        // Step 2: Call the Insert Affiliate SDK with the context and referring link
                        InsertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, "" + branchUniversalObject.getContentMetadata().convertToJson().get("~referring_link"));
                    } catch (JSONException e) {
                        // ... //
                    }
                }
            }
        }).withData(this.getIntent().getData()).init();
    }
}
```

### Using the SDK with AppsFlyer (Android)

To set up deep linking with AppsFlyer, follow these steps:

1. Create a [OneLink](https://support.appsflyer.com/hc/en-us/articles/208874366-Create-a-OneLink-link-for-your-campaigns) in AppsFlyer and pass it to our dashboard when an affiliate signs up.
   - Example: [Create Affiliate](https://docs.insertaffiliate.com/create-affiliate).
2. Initialize AppsFlyer SDK and set up deep link handling in your app.

#### Prerequisites

- AppsFlyer Dev Key from your AppsFlyer dashboard
- Android package name configured in AppsFlyer
- Minimum Android Gradle Plugin 7.0+

#### Install & Configure Dependencies

1. **Add AppsFlyer SDK** to your app's `build.gradle`:

```java
dependencies {
    implementation 'com.appsflyer:af-android-sdk:6+'
    implementation 'com.android.installreferrer:installreferrer:2.2'
    implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.1.4'
}
```

2. **Sync your project** to ensure dependencies are downloaded.

#### Manifest Setup

1. **Add required permissions** to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

2. **Configure intent filters** for OneLink deep linking in your `MainActivity`:

```xml
<activity android:name=".MainActivity" android:exported="true">
    <!-- OneLink deep linking -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data 
            android:scheme="https" 
            android:host="{{ONELINK_SUBDOMAIN}}.onelink.me" />
    </intent-filter>
    
    <!-- Custom scheme (optional) -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data 
            android:scheme="{{SCHEME}}" 
            android:host="open" />
    </intent-filter>
</activity>
```

3. **Add AppsFlyer metadata** to your `<application>` tag:

```xml
<application>
    <meta-data
        android:name="com.appsflyer.ApiKey"
        android:value="{{APPSFLYER_DEV_KEY}}" />
</application>
```

#### Initialize AppsFlyer and the SDK

1. **Create an Application class** if you don't have one:

```java
import com.appsflyer.AppsFlyerLib;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class {{APPLICATION_CLASS}} extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Insert Affiliate SDK
        InsertAffiliateManager.init(this, "{{your_company_code}}");
        
        // Initialize AppsFlyer
        AppsFlyerLib.getInstance().init("{{APPSFLYER_DEV_KEY}}", null, this);
        AppsFlyerLib.getInstance().start(this);
    }
}
```

2. **Register your Application class** in `AndroidManifest.xml`:

```xml
<application android:name=".{{APPLICATION_CLASS}}">
```

#### Handle Deep Links/Attribution

**Example with RevenueCat:**

```java
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.revenuecat.purchases.Purchases;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {
    private InsertAffiliateManager insertAffiliateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        insertAffiliateManager = new InsertAffiliateManager(this);
        
        // Handle intent deep links for direct app launch
        handleIntent(getIntent());
        
        // Set up AppsFlyer attribution listener
        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onConversionDataSuccess(java.util.Map<String, Object> conversionData) {
                // Handle install attribution data if needed
            }

            @Override
            public void onConversionDataFail(String errorMessage) {
                // Handle error
            }

            @Override
            public void onAppOpenAttribution(java.util.Map<String, String> attributionData) {
                handleAttributionData(attributionData);
            }

            @Override
            public void onAttributionFailure(String errorMessage) {
                // Handle error
            }
        };

        AppsFlyerLib.getInstance().registerConversionListener(this, conversionListener);
    }

    private void handleAttributionData(java.util.Map<String, String> attributionData) {
        // Extract deep link from AppsFlyer data
        String link = attributionData.get("af_dp");
        if (link == null || link.isEmpty()) {
            link = attributionData.get("af_deeplink");
        }
        if (link == null || link.isEmpty()) {
            link = attributionData.get("link");
        }

        if (link != null && !link.isEmpty()) {
            // Pass to Insert Affiliate SDK
            insertAffiliateManager.setInsertAffiliateIdentifier(this, link);
            
            // Set RevenueCat attribute
            String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(this);
            if (affiliateId != null && !affiliateId.isEmpty()) {
                java.util.Map<String, String> attributes = new java.util.HashMap<>();
                attributes.put("insert_affiliate", affiliateId);
                Purchases.getSharedInstance().setAttributes(attributes);
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String deepLink = intent.getData().toString();
            insertAffiliateManager.setInsertAffiliateIdentifier(this, deepLink);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
}
```

**Example with Iaptic/Google Play Direct:**

```java
public class MainActivity extends AppCompatActivity {
    private InsertAffiliateManager insertAffiliateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        insertAffiliateManager = new InsertAffiliateManager(this);
        handleIntent(getIntent());
        
        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onConversionDataSuccess(java.util.Map<String, Object> conversionData) {
                // Handle install attribution
            }

            @Override
            public void onConversionDataFail(String errorMessage) {
                // Handle error
            }

            @Override
            public void onAppOpenAttribution(java.util.Map<String, String> attributionData) {
                String link = attributionData.get("af_dp");
                if (link == null || link.isEmpty()) {
                    link = attributionData.get("af_deeplink");
                }
                if (link == null || link.isEmpty()) {
                    link = attributionData.get("link");
                }

                if (link != null && !link.isEmpty()) {
                    insertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, link);
                }
            }

            @Override
            public void onAttributionFailure(String errorMessage) {
                // Handle error
            }
        };

        AppsFlyerLib.getInstance().registerConversionListener(this, conversionListener);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String deepLink = intent.getData().toString();
            insertAffiliateManager.setInsertAffiliateIdentifier(this, deepLink);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
}
```

#### SHA-256 Certificate & App Links

1. **Get your signing certificate SHA-256** fingerprint:

```bash
# Debug keystore
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | grep SHA256

# Release keystore (replace with your keystore path)
keytool -list -v -alias your-key-alias -keystore /path/to/your-keystore.jks
```

2. **Configure in AppsFlyer OneLink:**
   - Add your `{{PACKAGE_NAME}}` to Associated Apps
   - Add the SHA-256 fingerprint from step 1
   - For Play App Signing, use the SHA-256 from Google Play Console → Setup → App integrity

#### Verifying the Integration

1. **Test deep link** on a connected device:

```bash
adb shell am start -a android.intent.action.VIEW -d "https://{{ONELINK_SUBDOMAIN}}.onelink.me/{{LINK_ID}}/test"
```

2. **Check logs** for successful attribution:

```bash
adb logcat | grep -E "(AppsFlyer|Insert Affiliate)"
```

3. **Verify affiliate identifier** is set:

```java
String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(this);
Log.d("InsertAffiliate", "Current affiliate ID: " + affiliateId);
```

#### Troubleshooting

- **App opens Play Store instead of app**: Check that your package name and SHA-256 are correctly configured in AppsFlyer OneLink settings
- **No attribution data received**: Ensure `registerConversionListener` is called before `AppsFlyerLib.getInstance().start()`
- **Deep links not working**: Verify intent-filter configuration and test with `adb` command above

| Placeholder | Example/Note |
|-------------|--------------|
| `{{APPSFLYER_DEV_KEY}}` | Your AppsFlyer Dev Key |
| `{{APPLICATION_CLASS}}` | e.g., MyApplication |
| `{{PACKAGE_NAME}}` | e.g., com.yourcompany.yourapp |
| `{{ONELINK_SUBDOMAIN}}` | e.g., yourapp (from yourapp.onelink.me) |
| `{{SCHEME}}` | Custom scheme if applicable (e.g., myapp) |
```

## Additional Features

### 1: Event Tracking (Beta)

The **InsertAffiliateAndroid SDK** now includes a beta feature for event tracking. Use event tracking to log key user actions such as signups, purchases, or referrals. This is useful for:
- Understanding user behaviour.
- Measuring the effectiveness of marketing campaigns.
- Incentivising affiliates for designated actions being taken by the end users, rather than just in app purchases (i.e. pay an affilaite for each signup).

#### Using `trackEvent`

To track an event, use the `trackEvent` function. Make sure to set an affiliate identifier first; otherwise, event tracking won’t work. Here’s an example:

```java
InsertAffiliateManager.trackEvent(activity, "your_event_name");
```


### 2: Short Codes (Beta)

#### What are Short Codes?

Short codes are unique, 3 to 25 character alphanumeric identifiers that affiliates can use to promote products or subscriptions. These codes are ideal for influencers or partners, making them easier to share than long URLs.

**Example Use Case**: An influencer promotes a subscription with the short code "JOIN123456" within their TikTok video's description. When users enter this code within your app during sign-up or before purchase, the app tracks the subscription back to the influencer for commission payouts.

For more information, visit the [Insert Affiliate Short Codes Documentation](https://docs.insertaffiliate.com/short-codes).


#### Setting a Short Code

Use the `setShortCode` method to associate a short code with an affiliate. This is ideal for scenarios where users enter the code via an input field, pop-up, or similar UI element.

Short codes must meet the following criteria:
- Between **3 and 25 characters long**.
- Contain only **letters and numbers** (alphanumeric characters).
- Replace {{ user_entered_short_code }} with the short code the user enters through your chosen input method, i.e. an input field / pop up element



##### Example Usage
Set the Affiliate Identifier (required for tracking):

```java
InsertAffiliateManager.setShortCode(activity, "JOIN123456");
```

### 3: Discounts for Users → Offer Codes / Dynamic Product IDs

The SDK allows you to apply dynamic modifiers to in-app purchases based on whether the app was installed via an affiliate. These modifiers can be used to swap the default product ID for a discounted or trial-based one - similar to applying an offer code.

#### How It Works

When a user clicks an affiliate link or enters a short code linked to an offer (set up in the **Insert Affiliate Dashboard**), the SDK auto-populates the `OfferCode` field with a relevant modifier (e.g., `_oneWeekFree`). You can append this to your base product ID to dynamically display the correct subscription.

#### Basic Usage

##### 1. Automatic Offer Code Fetching
If an affiliate short code is stored, the SDK automatically fetches and saves the associated offer code modifier.

##### 2. Access the Offer Code Modifier
The offer code modifier is available through the context:

```java
// Get the stored offer code modifier (returns null if none exists)
String offerCodeModifier = InsertAffiliateManager.getStoredOfferCode(activity);

if (offerCodeModifier != null) {
    Log.i("MyApp", "Offer code modifier found: " + offerCodeModifier);
    // Use the modifier to create a dynamic product ID
} else {
    Log.i("MyApp", "No offer code modifier found, using default product");
}
```

##### Setup Requirements

#### Insert Affiliate Setup Instructions

1. Go to your Insert Affiliate dashboard at [app.insertaffiliate.com/affiliates](https://app.insertaffiliate.com/affiliates)
2. Select the affiliate you want to configure
3. Click "View" to access the affiliate's settings
4. Assign an iOS IAP Modifier to the affiliate (e.g., `_oneWeekFree`, `_threeMonthsFree`)
5. Assign an Android IAP Modifier to the affiliate (e.g., `-oneweekfree`, `-threemonthsfree`)
5. Save the settings

Once configured, when users click that affiliate's links or enter their short codes, your app will automatically receive the modifier and can load the appropriate discounted product.

#### Google Play Console Configuration
There are multiple ways you can configure your products in Google Play Console:

1. **Multiple Products Approach**: Create both a base and a promotional product:
   - Base product: `oneMonthSubscription`
   - Promo product: `oneMonthSubscription-oneweekfree`

2. **Single Product with Multiple Base Plans**: Create one product with multiple base plans, one with an offer attached

3. **Developer Triggered Offers**: Have one base product and apply the offer through developer-triggered offers

4. **Base Product with Intro Offers**: Have one base product that includes an introductory offer

Any of these approaches are suitable and work with the SDK. The important part is that your product naming follows the pattern where the offer code modifier can be appended to identify the promotional version.

**If using the Multiple Products Approach:**
- Ensure **both** products are activated and available for purchase.
- Generate a release to at least **Internal Testing** to make the products available in your current app build

**Product Naming Pattern:**
- Follow the pattern: `{baseProductId}{OfferCode}`
- Example: `oneMonthSubscription` + `_oneWeekFree` = `oneMonthSubscription_oneWeekFree`

---

#### RevenueCat Dashboard Configuration (Android)

If using RevenueCat for Android:

1. Create separate offerings:
   - Base offering: `premium_monthly`
   - Modified offering: `premium_monthly_oneWeekFree`

2. Add both product IDs under different offerings in RevenueCat.

3. Ensure modified products follow this naming pattern: `{baseProductId}{cleanOfferCode}`. e.g. `premium_monthly_oneWeekFree`

### Android Integration Examples

#### Example 1: Google Play Billing Library Integration

```java
import com.android.billingclient.api.*;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;
import java.util.*;

public class SubscriptionManager {
    private static final String BASE_PRODUCT_ID = "oneMonthSubscription";
    private BillingClient billingClient;
    private Activity activity;
    private List<SkuDetails> availableProducts = new ArrayList<>();

    public SubscriptionManager(Activity activity) {
        this.activity = activity;
        initializeBillingClient();
    }

    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchaseUpdateListener)
            .enablePendingPurchases()
            .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    fetchAvailableProducts();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request
            }
        });
    }

    public void fetchAvailableProducts() {
        // Get offer code from Insert Affiliate SDK
        String offerCode = InsertAffiliateManager.getStoredOfferCode(activity);
        
        List<String> skuList = new ArrayList<>();
        String dynamicProductId = BASE_PRODUCT_ID;
        
        if (offerCode != null && !offerCode.isEmpty()) {
            // Construct dynamic product ID with offer code
            dynamicProductId = BASE_PRODUCT_ID + offerCode;
            skuList.add(dynamicProductId);
            Log.i("SubscriptionManager", "Looking for promotional product: " + dynamicProductId);
        }
        
        // Always include base product as fallback
        skuList.add(BASE_PRODUCT_ID);

        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);

        billingClient.querySkuDetailsAsync(params.build(),
            new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult,
                                               List<SkuDetails> skuDetailsList) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        availableProducts = skuDetailsList;
                        
                        // Sort to prioritize promotional products
                        if (offerCode != null) {
                            String finalDynamicProductId = dynamicProductId;
                            availableProducts.sort((a, b) -> 
                                a.getSku().equals(finalDynamicProductId) ? -1 : 1);
                        }
                        
                        displayProducts();
                    } else {
                        Log.e("SubscriptionManager", "Failed to query SKU details: " + 
                              billingResult.getDebugMessage());
                        // Fallback to base product only
                        fetchBaseProductOnly();
                    }
                }
            });
    }

    private void fetchBaseProductOnly() {
        List<String> skuList = Arrays.asList(BASE_PRODUCT_ID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);

        billingClient.querySkuDetailsAsync(params.build(),
            new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult,
                                               List<SkuDetails> skuDetailsList) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        availableProducts = skuDetailsList;
                        displayProducts();
                    }
                }
            });
    }

    private void displayProducts() {
        // Update UI with available products
        for (SkuDetails skuDetails : availableProducts) {
            Log.i("SubscriptionManager", "Available product: " + skuDetails.getSku() + 
                  " - " + skuDetails.getPrice());
        }
        
        String offerCode = InsertAffiliateManager.getStoredOfferCode(activity);
        if (offerCode != null && !availableProducts.isEmpty()) {
            String primaryProductId = availableProducts.get(0).getSku();
            if (primaryProductId.contains(offerCode)) {
                Log.i("SubscriptionManager", "🎉 Promotional pricing applied!");
                // Show special offer UI
            }
        }
    }

    public void launchPurchaseFlow() {
        if (!availableProducts.isEmpty()) {
            SkuDetails skuDetails = availableProducts.get(0); // Use primary product
            
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
                
            BillingResult billingResult = billingClient.launchBillingFlow(activity, flowParams);
            
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e("SubscriptionManager", "Failed to launch purchase flow: " + 
                      billingResult.getDebugMessage());
            }
        }
    }

    private PurchasesUpdatedListener purchaseUpdateListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.i("SubscriptionManager", "User canceled the purchase");
            } else {
                Log.e("SubscriptionManager", "Purchase failed: " + billingResult.getDebugMessage());
            }
        }
    };

    private void handlePurchase(Purchase purchase) {
        // Store the expected transaction for Insert Affiliate tracking
        InsertAffiliateManager.storeExpectedPlayStoreTransaction(activity, purchase.getPurchaseToken());
        
        // Acknowledge the purchase
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                        
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                        Log.i("SubscriptionManager", "Purchase acknowledged");
                    }
                });
            }
        }
    }
}
```

#### Example 2: RevenueCat Integration with Offer Codes

```java
import com.revenuecat.purchases.*;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class RevenueCatSubscriptionManager {
    private static final String BASE_OFFERING = "premium_monthly";
    private Activity activity;

    public RevenueCatSubscriptionManager(Activity activity) {
        this.activity = activity;
    }

    public void fetchOfferingsAndPurchase() {
        // Get offer code from Insert Affiliate SDK
        String offerCode = InsertAffiliateManager.getStoredOfferCode(activity);
        
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                Offering targetOffering = null;
                String targetOfferingKey = BASE_OFFERING;
                
                if (offerCode != null && !offerCode.isEmpty()) {
                    // Try to find promotional offering
                    String promoOfferingKey = BASE_OFFERING + offerCode;
                    targetOffering = offerings.get(promoOfferingKey);
                    
                    if (targetOffering != null) {
                        Log.i("RevenueCat", "Found promotional offering: " + promoOfferingKey);
                        targetOfferingKey = promoOfferingKey;
                    } else {
                        Log.i("RevenueCat", "Promotional offering not found, using base offering");
                    }
                }
                
                // Fallback to base offering if promo not found
                if (targetOffering == null) {
                    targetOffering = offerings.get(BASE_OFFERING);
                }
                
                if (targetOffering != null && !targetOffering.getAvailablePackages().isEmpty()) {
                    displayOfferingAndPurchase(targetOffering, offerCode != null);
                } else {
                    Log.e("RevenueCat", "No offerings available");
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e("RevenueCat", "Error fetching offerings: " + error.getMessage());
            }
        });
    }

    private void displayOfferingAndPurchase(Offering offering, boolean isPromotional) {
        Package packageToPurchase = offering.getAvailablePackages().get(0);
        
        if (isPromotional) {
            Log.i("RevenueCat", "🎉 Special offer applied!");
            // Update UI to show promotional pricing
        }
        
        Log.i("RevenueCat", "Product: " + packageToPurchase.getProduct().getTitle() + 
              " - " + packageToPurchase.getProduct().getPrice());

        // Launch purchase
        Purchases.getSharedInstance().purchasePackage(
            activity,
            packageToPurchase,
            new PurchaseCallback() {
                @Override
                public void onCompleted(@NonNull StoreTransaction storeTransaction, @NonNull CustomerInfo customerInfo) {
                    Log.i("RevenueCat", "Purchase completed successfully");
                    // Purchase successful - RevenueCat handles the rest via webhook
                }

                @Override
                public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                    if (userCancelled) {
                        Log.i("RevenueCat", "User cancelled purchase");
                    } else {
                        Log.e("RevenueCat", "Purchase failed: " + error.getMessage());
                    }
                }
            }
        );
    }
}

## Attribution Timeout Control (New)

Control how long affiliate attribution remains active after a user clicks an affiliate link. This feature allows you to set expiration periods for affiliate tracking.

### Configuration

Set the timeout during SDK initialization:

```java
// Set 7-day attribution timeout (604800 seconds)
InsertAffiliateManager.init(
    activity,                    // Your activity context
    "{{ your_company_code }}",   // Your company code
    false,                       // Enable verbose logging (optional)
    false,                       // Enable insert links (optional)
    604800                       // Affiliate attribution timeout in seconds (7 days)
);
```

### Available Methods

**Get affiliate identifier with timeout control:**
```java
// Get identifier respecting timeout (returns null if expired)
String identifier = InsertAffiliateManager.returnInsertAffiliateIdentifier(activity);

// Get identifier ignoring timeout
String identifier = InsertAffiliateManager.returnInsertAffiliateIdentifier(activity, true);
```

**Check if attribution is valid:**
```java
boolean isValid = InsertAffiliateManager.isAffiliateAttributionValid(activity);
if (isValid) {
    // Attribution is still active
} else {
    // Attribution has expired or no timeout is configured
}
```

**Get when affiliate was stored:**
```java
long storedDate = InsertAffiliateManager.getAffiliateStoredDate(activity);
if (storedDate > 0) {
    // Date as seconds since epoch
    Date date = new Date(storedDate * 1000);
}
```

### Default Behavior

- **No timeout by default**: Attribution never expires when timeout is set to `0` (default)
- **Custom timeout**: When set to a positive value, attribution expires after that many seconds
- **Expired attributions**: Return `null` when calling `returnInsertAffiliateIdentifier(activity)`
- **Override timeout**: Use `returnInsertAffiliateIdentifier(activity, true)` to ignore timeout and get identifier anyway

### Common Timeout Values

```java
// 1 day
InsertAffiliateManager.init(activity, "company_code", 86400);

// 7 days (recommended)
InsertAffiliateManager.init(activity, "company_code", 604800);

// 30 days
InsertAffiliateManager.init(activity, "company_code", 2592000);

// No timeout (default)
InsertAffiliateManager.init(activity, "company_code", 0);
```