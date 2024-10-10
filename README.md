# Insert Affiliate SDK

A brief description of your SDK, its purpose, and what it offers.

## Table of Contents

1. [Installation](#installation)
2. [In App Purchase Setup](#in-app-purchase-setup)
3. [Deep link Setup](#deep-link-setup)
   
## Features

- List the key features of your SDK.
- Highlight any unique aspects or functionalities.

## Installation

Instructions on how to install the SDK. Include different methods if applicable (e.g., Maven, Gradle, manual download).

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```bash
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
 ```

Step 2. Add the dependency

```bash
dependencies {
	        implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.0.1'
}
```

## In App Purchase Setup

```bash
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

        //Copy This code to your main activity
        insertAffiliateManager = new InsertAffiliateManager(MainActivity.this);
        insertAffiliateManager.init(MainActivity.this);

}
```
~ In App Purchase Update (Call callApiForValidate method when user purchase was done successfully)
```bash
public class InAppFragment extends Fragment {
    InsertAffiliateManager insertAffiliateManager;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);
        insertAffiliateManager = new InsertAffiliateManager(getActivity());

	BillingClient billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(
                        new PurchasesUpdatedListener() {
                            @Override
                            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
                               if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK && list !=null) {
                                   for (Purchase purchase: list){
                                       verifySubPurchase(purchase);
                                   }
                               }
                            }
                        }
                ).build();
}

void verifySubPurchase(Purchase purchases) {

        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchases.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    String orderId = purchases.getOrderId();

                insertAffiliateManager.callApiForValidate(getActivity(),
                            getActivity().getPackageName(),
                            "Iaptic Secret Key",
                            purchases.getProduct(), // Subscription Id
                            orderId, // Order Id
                            purchases.getPurchaseToken(), // Purchase Token
                            purchases.getOriginalJson(), // Orignal JSON Data
                            purchases.getSignature()); // Purchase Signature

				}
        });

    }

}
```

## Deep Link Setup

Step 1. Add the dependency

```bash
dependencies {
    implementation 'io.branch.sdk.android:library:5.8.0' // Check for latest version before hard-coding
}
```

Step 2. Enable Auto Instance Of Branch Io On Your Application Class

```bash
import android.app.Application;

import io.branch.referral.Branch;

public class InsertAffiliateApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Branch.enableLogging();
        Branch.getAutoInstance(this);
    }
}
```

Step 3. Start Session For Branch Io And Pass Unique Id Of User For Session

```bash
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

        insertAffiliateManager = new InsertAffiliateManager(MainActivity.this);
        insertAffiliateManager.init(MainActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Branch.getInstance().setRequestMetadata("$analytics_visitor_id", InsertAffiliateManager.getUniqueId(MainActivity.this));
        Branch.sessionBuilder(this).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null && branchUniversalObject != null) {
                    try {
                        InsertAffiliateManager.saveReferLink(MainActivity.this, "" + branchUniversalObject.getContentMetadata().convertToJson().get("~referring_link"));
                    } catch (JSONException e) {
                        // TODO: Handle exception if desired
                    }
                }
            }
        }).withData(this.getIntent().getData()).init();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
```

Step 4. Edit Manifest File And Add Branch Io Link

```bash
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    <uses-feature android:name="android.hardware.screen.portrait" android:required="false" />
    <uses-feature android:name="android.hardware.telephony" android:required="false" />

    <application
        android:name=".InsertAffiliateApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Insertaffiliate"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">
        <activity
            android:name=".SplashScreen"
            android:exported="true"
            android:label="@string/app_name"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>


        </activity>
        <activity
            android:name=".MainActivity"
            android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize"
            android:exported="true"
            android:label="@string/app_name">

            <intent-filter>
                <!-- If utilizing $deeplink_path please explicitly declare your hosts, or utilize a wildcard(*) -->
                <!-- REPLACE `android:scheme` with your Android URI scheme -->
                <data
                    android:host="open"
                    android:scheme="Domain Name" />
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
            </intent-filter>

            <!-- Branch App Links - Live App -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <!-- REPLACE `android:host` with your `app.link` domain -->
                <data
                    android:host="Insert Your Branch Io Domain URL"
                    android:scheme="https" />
                <!-- REPLACE `android:host` with your `-alternate` domain (required for proper functioning of App Links and Deepviews) -->
                <data
                    android:host="Insert Your Branch Io Domain URL"
                    android:scheme="https" />
            </intent-filter>
            <meta-data
                android:name="android.webkit.WebView.EnableSafeBrowsing"
                android:value="true" />
        </activity>

        <meta-data
            android:name="io.branch.sdk.BranchKey"
            android:value="Branch Io Key" />
        <meta-data
            android:name="io.branch.sdk.TestMode"
            android:value="false" />
    </application>
    <queries>
        <intent>
            <action android:name="android.intent.action.SEND" />
            <data android:mimeType="text/plain" />
        </intent>
    </queries>
</manifest>
```
