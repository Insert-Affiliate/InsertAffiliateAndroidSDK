# Insert Affiliate SDK

The **InsertAffiliateAndroid SDK** is designed for Android applications, providing seamless integration with the [Insert Affiliate platform](https://insertaffiliate.com). This SDK enables functionalities such as managing affiliate links, handling in-app purchases (IAP), and utilizing deep links. For more details and to access the Insert Affiliate dashboard, visit [app.insertaffiliate.com](https://app.insertaffiliate.com).

## Table of Contents

1. [Installation](#installation)
2. [In App Purchase Setup](#in-app-purchase-setup)
3. [Deep link Setup](#deep-link-setup)
   
## Features

- **Unique Device Identification**: Generates and stores a short unique device ID to identify users effectively.
- **Affiliate Identifier Management**: Set and retrieve the affiliate identifier based on user-specific links.
- **In-App Purchase (IAP) Initialisation**: Easily reinitialise in-app purchases with validation options using the affiliate identifier.

## Installation

Follow the steps below to install the SDK. You can use different methods depending on your project setup (e.g., Gradle, Maven, or manual download).

### Step 1: Add the JitPack repository

In your root `build.gradle`, add the JitPack repository to the `repositories` section:

```java
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2. Add the SDK dependency

In your module's build.gradle, add the SDK dependency:

```java
dependencies {
    implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.0.1'
}
```

## In-App Purchase Setup
### Step 1: Initialise the InsertAffiliateManager in `MainActivity`

In your `MainActivity`, add the following code to initialise the `InsertAffiliateManager` and set up your in-app purchases:

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
        insertAffiliateManager.init(MainActivity.this);

}
```

## Step 2: Handle the In-App Purchase and Validate

After a user makes a successful purchase, you need to verify and acknowledge the purchase. Add this code in your `InAppFragment` to handle the purchase flow and validate it through `InsertAffiliateManager`.

- Replace `{{ your_iaptic_app_name }}` with your **Iaptic App Name**. You can find this [here](https://www.iaptic.com/account).
- Replace `{{ your_iaptic_secret_key }}` with your **Iaptic Secret Key**. You can find this [here](https://www.iaptic.com/settings).

Here's the code with placeholders for you to swap out:

```java
public class InAppFragment extends Fragment {
    InsertAffiliateManager insertAffiliateManager;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);
        insertAffiliateManager = new InsertAffiliateManager(getActivity());

        // Set up the BillingClient to listen for purchase updates
        BillingClient billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener(
                new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
                        if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK && list !=null) {
                            for (Purchase purchase: list){
                                verifySubPurchase(purchase); // Validate the purchase
                            }
                        }
                    }
                }
            ).build();
    }

    // Method to verify the purchase and acknowledge it
    void verifySubPurchase(Purchase purchases) {
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchases.getPurchaseToken())
            .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                String orderId = purchases.getOrderId();

                // Call API to validate the purchase
                insertAffiliateManager.callApiForValidate(
                    getActivity(),
                    "{{ your_iaptic_app_name }}",
                    "{{ your_iaptic_secret_key }}",
                    purchases.getProduct(),
                    orderId,
                    purchases.getPurchaseToken(),
                    purchases.getOriginalJson(),
                    purchases.getSignature()
                );
            }
        });
    }
}
```

## Deep Link Setup

### Step 1: Add the Deep Linking Platform Dependency

In this example, the deep linking functionality is implemented using Branch.io. Add the following dependency to your build.gradle file.

```java
dependencies {
    implementation 'io.branch.sdk.android:library'
}
```

### Step 2: Enable Auto-Initialisation of Branch.io in the Application Class

Create or modify your `Application` class to automatically initialise Branch.io when your app starts:

```java
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

### Step 3: Start Branch.io Session and Pass User's Unique ID

In your `MainActivity`, start a Branch.io session when the app is opened, and pass the user's unique ID for tracking. Add the following code in the `onStart()` method:

```java
public class MainActivity extends AppCompatActivity {
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
        // Set user metadata for Branch session
        Branch.getInstance().setRequestMetadata("$analytics_visitor_id", InsertAffiliateManager.getUniqueId(MainActivity.this));

        // Start Branch session and handle deep link callbacks
        Branch.sessionBuilder(this).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null && branchUniversalObject != null) {
                    try {
                        // Save the referring link from the deep link data
                        InsertAffiliateManager.saveReferLink(MainActivity.this, "" + branchUniversalObject.getContentMetadata().convertToJson().get("~referring_link"));
                    } catch (JSONException e) {
                        // Handle exception if necessary
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