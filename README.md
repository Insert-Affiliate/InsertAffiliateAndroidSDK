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
    implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.1.3'
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


### 2. Short Codes (Beta)

### What are Short Codes?

Short codes are unique, 3 to 25 character alphanumeric identifiers that affiliates can use to promote products or subscriptions. These codes are ideal for influencers or partners, making them easier to share than long URLs.

**Example Use Case**: An influencer promotes a subscription with the short code "JOIN123456" within their TikTok video's description. When users enter this code within your app during sign-up or before purchase, the app tracks the subscription back to the influencer for commission payouts.

For more information, visit the [Insert Affiliate Short Codes Documentation](https://docs.insertaffiliate.com/short-codes).


### Setting a Short Code

Use the `setShortCode` method to associate a short code with an affiliate. This is ideal for scenarios where users enter the code via an input field, pop-up, or similar UI element.

Short codes must meet the following criteria:
- Between **3 and 25 characters long**.
- Contain only **letters and numbers** (alphanumeric characters).
- Replace {{ user_entered_short_code }} with the short code the user enters through your chosen input method, i.e. an input field / pop up element



#### Example Usage
Set the Affiliate Identifier (required for tracking):

```java
InsertAffiliateManager.setShortCode(activity, "JOIN123456");
```
