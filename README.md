# Insert Affiliate Android SDK

[![Version](https://jitpack.io/v/Insert-Affiliate/InsertAffiliateAndroidSDK.svg)](https://jitpack.io/#Insert-Affiliate/InsertAffiliateAndroidSDK)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

The official Android SDK for [Insert Affiliate](https://insertaffiliate.com) - track affiliate-driven in-app purchases and reward your partners automatically.

**What does this SDK do?** It connects your Android app to Insert Affiliate's platform, enabling you to track which affiliates drive subscriptions and automatically pay them commissions when users make in-app purchases.

## 📋 Table of Contents

- [Quick Start (5 Minutes)](#-quick-start-5-minutes)
- [Essential Setup](#%EF%B8%8F-essential-setup)
  - [1. Initialize the SDK](#1-initialize-the-sdk)
  - [2. Configure In-App Purchase Verification](#2-configure-in-app-purchase-verification)
  - [3. Set Up Deep Linking](#3-set-up-deep-linking)
- [Verify Your Integration](#-verify-your-integration)
- [Advanced Features](#-advanced-features)
- [Troubleshooting](#-troubleshooting)
- [Support](#-support)

---

## 🚀 Quick Start (5 Minutes)

Get up and running with minimal code to validate the SDK works before tackling IAP and deep linking setup.

### Prerequisites

- **Android 5.0+** (API level 21 or higher)
- **Android Gradle Plugin 8.1+** and **Gradle 8.0+**
- **Company Code** from your [Insert Affiliate dashboard](https://app.insertaffiliate.com/settings)

### Installation

**Step 1:** Add JitPack repository to your **root** `build.gradle`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2:** Add the SDK dependency to your **module's** `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.2.0'
}
```

### Your First Integration

Add this minimal code to your `MainActivity.java` to test the SDK:

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SDK with verbose logging (recommended during setup)
        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",  // Get from https://app.insertaffiliate.com/settings
            true                  // Enable verbose logging for setup
        );
    }
}
```

**Expected Console Output:**

When the SDK initializes successfully, you'll see these logs:

```
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] Starting SDK initialization...
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] Company code provided: Yes
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] Verbose logging enabled
I/InsertAffiliate TAG: [Insert Affiliate] SDK initialized with company code: YOUR_COMPANY_CODE
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] Generated and saved new user ID: a1b2c3
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] SDK initialization completed
```

✅ **If you see these logs, the SDK is working!** Now proceed to Essential Setup below.

⚠️ **Disable verbose logging in production** by setting the third parameter to `false` or omitting it.

---

## ⚙️ Essential Setup

Complete these three required steps to start tracking affiliate-driven purchases.

### 1. Initialize the SDK

The SDK must be initialized in your `MainActivity` before using any features. You've already done the basic initialization above, but here are additional options:

#### Basic Initialization (Recommended for Getting Started)

```java
// Minimal setup with verbose logging enabled (recommended during development)
InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", true);
```

<details>
<summary><strong>Advanced Initialization Options</strong> (click to expand)</summary>

```java
// With Insert Links enabled (for Insert Affiliate's built-in deep linking)
InsertAffiliateManager.init(
    this,                    // Activity context
    "YOUR_COMPANY_CODE",     // Your company code
    true,                    // Enable verbose logging
    true                     // Enable Insert Links (includes install referrer)
);

// With attribution timeout (7 days = 604800 seconds)
InsertAffiliateManager.init(
    this,
    "YOUR_COMPANY_CODE",
    true,                    // Enable verbose logging
    false,                   // Insert Links disabled (if using 3rd party like Branch)
    604800                   // Attribution expires after 7 days
);
```

**Parameters:**
- `enableVerboseLogging`: Shows detailed logs for debugging (disable in production)
- `enableInsertLinks`: Set to `true` if using Insert Links, `false` if using Branch/AppsFlyer
- `affiliateAttributionActiveTimeSeconds`: How long affiliate attribution lasts (0 = never expires)

</details>

---

### 2. Configure In-App Purchase Verification

**Insert Affiliate requires a receipt verification method to validate purchases.** Choose **ONE** of the following:

| Method | Best For | Setup Time | Complexity |
|--------|----------|------------|------------|
| [**RevenueCat**](#option-1-revenuecat-recommended) | Most developers, managed infrastructure | ~10 min | ⭐ Simple |
| [**Adapty**](#option-2-adapty) | Paywall A/B testing, analytics | ~10 min | ⭐ Simple |
| [**Google Play Direct**](#option-3-google-play-direct) | Cost-focused users, no 3rd party fees | ~15 min | ⭐⭐ Medium |
| [**Iaptic**](#option-4-iaptic) | Custom requirements, direct control | ~20 min | ⭐⭐⭐ Advanced |

<details open>
<summary><h4>Option 1: RevenueCat (Recommended)</h4></summary>

**Step 1: Code Setup**

Complete the [RevenueCat Android SDK installation](https://www.revenuecat.com/docs/getting-started/installation/android) first, then modify your `MainActivity.java`:

```java
import com.revenuecat.purchases.Purchases;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Insert Affiliate SDK
        InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", true);

        // Pass affiliate identifier to RevenueCat
        String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(this);
        if (affiliateId != null) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("insert_affiliate", affiliateId);
            Purchases.getSharedInstance().setAttributes(attributes);
            Purchases.getSharedInstance().syncAttributesAndOfferingsIfNeededWith(
                error -> { /* handle error */ },
                offerings -> { /* offerings synced */ }
            );
        }
    }
}
```

**Expected Console Output:**
```
I/InsertAffiliate TAG: [Insert Affiliate] SDK initialized with company code: YOUR_COMPANY_CODE
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] Found identifier: SHORTCODE-a1b2c3
```

**Step 2: Webhook Setup**

1. In RevenueCat, [create a new webhook](https://www.revenuecat.com/docs/integrations/webhooks)
2. Configure webhook settings:
   - **Webhook URL**: `https://api.insertaffiliate.com/v1/api/revenuecat-webhook`
   - **Event Type**: "All events"
3. In your [Insert Affiliate dashboard](https://app.insertaffiliate.com/settings):
   - Set **In-App Purchase Verification** to `RevenueCat`
   - Copy the `RevenueCat Webhook Authentication Header` value
4. Back in RevenueCat webhook config:
   - Paste the authentication header value into the **Authorization header** field

✅ **RevenueCat setup complete!** Now skip to [Step 3: Set Up Deep Linking](#3-set-up-deep-linking)

</details>

<details>
<summary><h4>Option 2: Adapty</h4></summary>

**Step 1: Add Adapty Dependency**

Add Adapty to your **module's** `build.gradle`:

```gradle
dependencies {
    implementation 'io.adapty:android-sdk:3.3.0'
    implementation 'io.adapty:android-ui:3.3.0'
}
```

**Step 2: Initialize Adapty**

In your `Application` class (e.g., `MyApp.java`):

```java
import android.app.Application;
import com.adapty.Adapty;
import com.adapty.models.AdaptyConfig;
import com.adapty.utils.AdaptyLogLevel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyApp extends Application {
    // Track Adapty activation state to prevent double activation and fix race conditions
    private static final CountDownLatch adaptyActivationLatch = new CountDownLatch(1);
    private static final AtomicBoolean adaptyActivated = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();

        // Set log level before activation (optional)
        Adapty.setLogLevel(AdaptyLogLevel.VERBOSE);

        // Initialize Adapty with protection against double activation
        if (adaptyActivated.compareAndSet(false, true)) {
            Adapty.activate(
                getApplicationContext(),
                new AdaptyConfig.Builder("YOUR_ADAPTY_PUBLIC_KEY").build(),
                (AdaptyProfile profile) -> {
                    adaptyActivationLatch.countDown();  // Signal activation complete
                }
            );
        }
    }

    // Call this method to wait for Adapty activation before updating profile
    public static void waitForAdaptyActivation() {
        try {
            adaptyActivationLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**Step 3: Code Setup**

In your `MainActivity.java`:

```java
import com.adapty.Adapty;
import com.adapty.models.AdaptyProfileParameters;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Insert Affiliate SDK
        InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", true);

        // Set up callback for affiliate identifier changes - update Adapty when identifier changes
        InsertAffiliateManager.setInsertAffiliateIdentifierChangeCallback(
            new InsertAffiliateManager.InsertAffiliateIdentifierChangeCallback() {
                @Override
                public void onIdentifierChanged(String identifier) {
                    if (identifier != null && !identifier.isEmpty()) {
                        updateAdaptyProfile(identifier);
                    }
                }
            }
        );
    }

    private void updateAdaptyProfile(String affiliateId) {
        // Run on background thread to avoid blocking UI while waiting for Adapty
        new Thread(() -> {
            // Wait for Adapty activation before updating profile
            MyApp.waitForAdaptyActivation();

            AdaptyProfileParameters.Builder builder = new AdaptyProfileParameters.Builder()
                    .withCustomAttribute("insert_affiliate", affiliateId);

            Adapty.updateProfile(builder.build(), error -> {
                if (error != null) {
                    Log.e("MainActivity", "Failed to update Adapty profile: " + error.getMessage());
                } else {
                    Log.d("MainActivity", "Adapty profile updated with insert_affiliate: " + affiliateId);
                }
            });
        }).start();
    }
}
```

**Expected Console Output:**
```
D/MainActivity: Adapty profile updated with insert_affiliate: SHORTCODE-a1b2c3
```

**Step 4: Webhook Setup**

1. In your [Insert Affiliate dashboard](https://app.insertaffiliate.com/settings):
   - Set **In-App Purchase Verification** to `Adapty`
   - Copy the **Adapty Webhook URL**
   - Copy the **Adapty Webhook Authorization Header** value

2. In the [Adapty Dashboard](https://app.adapty.io/integrations):
   - Navigate to **Integrations** → **Webhooks**
   - Set **Production URL** to the webhook URL from Insert Affiliate
   - Set **Sandbox URL** to the same webhook URL
   - Paste the authorization header value into **Authorization header value**
   - Enable these options:
     - **Exclude historical events**
     - **Send attribution**
     - **Send trial price**
     - **Send user attributes**
   - Save the configuration

**Step 5: Verify Integration**

To confirm the affiliate identifier is set correctly:
1. Go to [app.adapty.io/profiles/users](https://app.adapty.io/profiles/users)
2. Find the test user who made a purchase
3. Look for `insert_affiliate` in **Custom attributes** with format: `{SHORT_CODE}-{UUID}`

✅ **Adapty setup complete!** Now skip to [Step 3: Set Up Deep Linking](#3-set-up-deep-linking)

</details>

<details>
<summary><h4>Option 3: Google Play Direct</h4></summary>

**Step 1: RTDN Setup**

Complete the [Real Time Developer Notifications setup](https://docs.insertaffiliate.com/direct-google-play-store-purchase-integration) in the Insert Affiliate documentation.

**Step 2: Code Implementation**

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class InAppFragment extends Fragment {

    @Override
    public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
        for (PurchaseInfo purchase : purchases) {
            purchasedInfoList.add(purchase);

            // Store expected transaction for backend validation
            InsertAffiliateManager.storeExpectedPlayStoreTransaction(
                getActivity(),
                purchase.getPurchaseToken()
            );
        }
    }
}
```

**Expected Console Output:**
```
I/InsertAffiliate TAG: [Insert Affiliate] Storing expected transaction: {"UUID":"token123...","companyCode":"YOUR_CODE","shortCode":"AFFILIATE1","storedDate":"2025-11-24T10:30:00Z"}
I/InsertAffiliate TAG: [Insert Affiliate] Expected transaction stored successfully.
```

✅ **Google Play Direct setup complete!** Now proceed to [Step 3: Set Up Deep Linking](#3-set-up-deep-linking)

</details>

<details>
<summary><h4>Option 4: Iaptic</h4></summary>

**Step 1: Code Setup**

Install the [Google In-App Billing Library](https://github.com/moisoni97/google-inapp-billing) (or your preferred billing library), then add this to your purchase handling code:

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class InAppFragment extends Fragment {
    InsertAffiliateManager insertAffiliateManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        insertAffiliateManager = new InsertAffiliateManager(getActivity());
        return view;
    }

    @Override
    public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
        for (PurchaseInfo purchase : purchases) {
            // Validate purchase with Iaptic via Insert Affiliate SDK
            insertAffiliateManager.validatePurchaseWithIapticAPI(
                getActivity(),
                "YOUR_IAPTIC_APP_NAME",     // From https://www.iaptic.com/account
                "YOUR_IAPTIC_PUBLIC_KEY",   // From https://www.iaptic.com/settings
                purchase.getProduct(),
                purchase.getOrderId(),
                purchase.getPurchaseToken(),
                purchase.getOriginalJson(),
                purchase.getSignature()
            );
        }
    }
}
```

**Step 2: Webhook Setup**

1. In your [Insert Affiliate dashboard settings](https://app.insertaffiliate.com/settings):
   - Set **In-App Purchase Verification** to `Iaptic`
   - Copy both `Iaptic Webhook URL` and `Iaptic Webhook Sandbox URL`
2. In your [Iaptic Settings](https://www.iaptic.com/settings):
   - Paste the webhook URLs into `Webhook URL` and `Sandbox Webhook URL` fields
   - Click **Save Settings**
3. Complete the [Iaptic Google Play Notifications setup](https://www.iaptic.com/documentation/setup/connect-with-google-publisher-api)

✅ **Iaptic setup complete!** Now proceed to [Step 3: Set Up Deep Linking](#3-set-up-deep-linking)

</details>

---

### 3. Set Up Deep Linking

**Deep linking lets affiliates share unique links that track users to your app.** Choose **ONE** deep linking provider:

| Provider | Best For | Complexity | Setup Guide |
|----------|----------|------------|-------------|
| [**Insert Links**](#option-1-insert-links-simplest) | Simple setup, no 3rd party | ⭐ Simple | [View](#option-1-insert-links-simplest) |
| [**Branch.io**](#option-2-branchio) | Robust attribution, deferred deep linking | ⭐⭐ Medium | [View](#option-2-branchio) |
| [**AppsFlyer**](#option-3-appsflyer) | Enterprise analytics, comprehensive attribution | ⭐⭐ Medium | [View](#option-3-appsflyer) |

<details open>
<summary><h4>Option 1: Insert Links (Simplest)</h4></summary>

Insert Links is Insert Affiliate's built-in deep linking solution—no third-party SDK required.

**Prerequisites:**
- Complete the [Insert Links setup](https://docs.insertaffiliate.com/insert-links) in the Insert Affiliate dashboard

**Code Implementation:**

Choose the example that matches your IAP verification platform:

**With RevenueCat:**

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;
import com.revenuecat.purchases.Purchases;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SDK with Insert Links enabled
        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",
            true,  // Verbose logging
            true   // Enable Insert Links
        );

        // Set up callback for affiliate identifier changes
        InsertAffiliateManager.setInsertAffiliateIdentifierChangeCallback(
            new InsertAffiliateManager.InsertAffiliateIdentifierChangeCallback() {
                @Override
                public void onIdentifierChanged(String identifier) {
                    if (identifier != null) {
                        Map<String, String> attributes = new HashMap<>();
                        attributes.put("insert_affiliate", identifier);
                        Purchases.getSharedInstance().setAttributes(attributes);
                        Purchases.getSharedInstance().syncAttributesAndOfferingsIfNeededWith(
                            error -> { /* handle error */ },
                            offerings -> { /* offerings synced */ }
                        );
                    }
                }
            }
        );

        // Handle deep link from app launch
        InsertAffiliateManager.handleInsertLink(this, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        InsertAffiliateManager.handleInsertLink(this, intent);
    }
}
```

**With Adapty:**

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;
import com.adapty.Adapty;
import com.adapty.models.AdaptyProfileParameters;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SDK with Insert Links enabled
        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",
            true,  // Verbose logging
            true   // Enable Insert Links
        );

        // Set up callback for affiliate identifier changes
        InsertAffiliateManager.setInsertAffiliateIdentifierChangeCallback(
            new InsertAffiliateManager.InsertAffiliateIdentifierChangeCallback() {
                @Override
                public void onIdentifierChanged(String identifier) {
                    if (identifier != null && !identifier.isEmpty()) {
                        updateAdaptyProfile(identifier);
                    }
                }
            }
        );

        // Handle deep link from app launch
        InsertAffiliateManager.handleInsertLink(this, getIntent());
    }

    private void updateAdaptyProfile(String affiliateId) {
        AdaptyProfileParameters.Builder builder = new AdaptyProfileParameters.Builder()
                .withCustomAttribute("insert_affiliate", affiliateId);

        Adapty.updateProfile(builder.build(), error -> {
            if (error != null) {
                Log.e("MainActivity", "Failed to update Adapty: " + error.getMessage());
            } else {
                Log.d("MainActivity", "Adapty updated with: " + affiliateId);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        InsertAffiliateManager.handleInsertLink(this, intent);
    }
}
```

**With Google Play Direct or Iaptic:**

```java
import com.aks.insertaffiliateandroid.InsertAffiliateManager;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SDK with Insert Links enabled
        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",
            true,  // Verbose logging
            true   // Enable Insert Links
        );

        // Set up callback for affiliate identifier changes
        InsertAffiliateManager.setInsertAffiliateIdentifierChangeCallback(
            new InsertAffiliateManager.InsertAffiliateIdentifierChangeCallback() {
                @Override
                public void onIdentifierChanged(String identifier) {
                    Log.i("InsertAffiliate", "Affiliate identifier stored: " + identifier);
                    // Identifier is stored automatically for direct store integration
                }
            }
        );

        // Handle deep link from app launch
        InsertAffiliateManager.handleInsertLink(this, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        InsertAffiliateManager.handleInsertLink(this, intent);
    }
}
```

**Expected Console Output:**
```
I/InsertAffiliate TAG: [Insert Affiliate] [VERBOSE] Insert links enabled: true
I/InsertAffiliate TAG: [Insert Affiliate] Deep link detected with insertAffiliate parameter: AFFILIATE1
I/InsertAffiliate TAG: [Insert Affiliate] Setting affiliate identifier.
I/InsertAffiliate TAG: [Insert Affiliate] Short link received: AFFILIATE1
I/InsertAffiliate TAG: [Insert Affiliate] Storing affiliate identifier: AFFILIATE1
```

✅ **Insert Links setup complete!** Skip to [Verify Your Integration](#-verify-your-integration)

</details>

<details>
<summary><h4>Option 2: Branch.io</h4></summary>

Branch.io provides robust attribution and deferred deep linking capabilities.

**Key Integration Steps:**
1. Install and configure [Branch SDK for Android](https://help.branch.io/developers-hub/docs/android-basic-integration)
2. Extract `~referring_link` from Branch callback
3. Pass to Insert Affiliate SDK using `setInsertAffiliateIdentifier()`

📖 **[View complete Branch.io integration guide →](docs/deep-linking-branch.md)**

✅ **After completing Branch setup**, skip to [Verify Your Integration](#-verify-your-integration)

</details>

<details>
<summary><h4>Option 3: AppsFlyer</h4></summary>

AppsFlyer provides enterprise-grade analytics and comprehensive attribution.

**Key Integration Steps:**
1. Install and configure [AppsFlyer SDK for Android](https://dev.appsflyer.com/hc/docs/android-sdk-reference-getting-started)
2. Create AppsFlyer OneLink in dashboard
3. Extract deep link from `onAppOpenAttribution()` callback
4. Pass to Insert Affiliate SDK using `setInsertAffiliateIdentifier()`

📖 **[View complete AppsFlyer integration guide →](docs/deep-linking-appsflyer.md)**

✅ **After completing AppsFlyer setup**, proceed to [Verify Your Integration](#-verify-your-integration)

</details>

---

## ✅ Verify Your Integration

Before going live, verify everything works correctly:

### Integration Checklist

- [ ] **SDK Initializes**: Check console for `SDK initialized with company code` log
- [ ] **Affiliate Identifier Stored**: Click a test affiliate link and verify console shows `Storing affiliate identifier: [CODE]`
- [ ] **Purchase Tracked**: Make a test purchase and verify transaction is sent to Insert Affiliate

### Testing Commands

**Test Deep Link (via ADB):**

```bash
# Replace with your actual deep link URL
adb shell am start -a android.intent.action.VIEW -d "https://your-app.onelink.me/abc123"
```

**Check Stored Affiliate Identifier:**

```java
String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(this);
Log.d("Test", "Current affiliate ID: " + affiliateId);
```

**Expected Output:** `Current affiliate ID: AFFILIATE1-a1b2c3`

### Common Setup Issues

| Issue | Solution |
|-------|----------|
| "Company code is not set" | Ensure `init()` is called before any other SDK methods |
| "No affiliate identifier found" | User must click an affiliate link before making a purchase |
| Deep link opens browser instead of app | Verify intent filters in `AndroidManifest.xml` and SHA-256 certificate |
| Purchase not tracked | Check webhook configuration in IAP verification platform |

---

## 🔧 Advanced Features

<details>
<summary><h3>Event Tracking (Beta)</h3></summary>

Track custom events beyond purchases (e.g., signups, referrals) to incentivize affiliates for specific actions.

```java
// Track custom event (affiliate identifier must be set first)
InsertAffiliateManager.trackEvent(this, "user_signup");
```

**Expected Console Output:**
```
I/InsertAffiliate TAG: [Insert Affiliate] Event tracked successfully
```

**Use Cases:**
- Pay affiliates for signups instead of purchases
- Track trial starts, content unlocks, or other conversions

</details>

<details>
<summary><h3>Short Codes</h3></summary>

Short codes are 3-25 character alphanumeric codes affiliates can share (e.g., "SAVE20" in a TikTok video description).

**Validate and Store Short Code:**

```java
InsertAffiliateManager.setShortCode(this, "SAVE20", new InsertAffiliateManager.ShortCodeValidationCallback() {
    @Override
    public void onValidationComplete(boolean isValid) {
        if (isValid) {
            Log.i("MyApp", "Short code is valid!");
            // Show success message to user
        } else {
            Log.e("MyApp", "Invalid short code");
            // Show error message
        }
    }
});
```

**Get Affiliate Details Without Setting:**

```java
InsertAffiliateManager.getAffiliateDetails("SAVE20", new InsertAffiliateManager.AffiliateDetailsCallback() {
    @Override
    public void onAffiliateDetailsReceived(InsertAffiliateManager.AffiliateDetails details) {
        if (details != null) {
            Log.i("MyApp", "Affiliate: " + details.getAffiliateName());
            // Display affiliate info to user
        }
    }
});
```

Learn more: [Short Codes Documentation](https://docs.insertaffiliate.com/short-codes)

</details>

<details>
<summary><h3>Dynamic Offer Codes / Discounts</h3></summary>

Automatically apply discounts or trials when users come from specific affiliates.

**How It Works:**
1. Configure an offer code modifier in your [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates) (e.g., `-oneweekfree`)
2. SDK automatically fetches and stores the modifier when affiliate identifier is set
3. Use the modifier to construct dynamic product IDs

**Quick Example:**

```java
String offerCode = InsertAffiliateManager.getStoredOfferCode(this);
if (offerCode != null) {
    // Construct dynamic product ID
    String productId = "oneMonthSubscription" + offerCode;
    // Result: "oneMonthSubscription-oneweekfree"
}
```

📖 **[View complete Dynamic Offer Codes guide →](docs/dynamic-offer-codes.md)**

Includes full examples for:
- Google Play Console setup (multiple products, base plans, developer offers)
- Google Play Billing integration with automatic product selection
- RevenueCat integration with dynamic offerings
- Testing and troubleshooting

</details>

<details>
<summary><h3>Attribution Timeout Control</h3></summary>

Control how long affiliate attribution remains active after a user clicks a link (e.g., 7-day attribution window).

**Set Timeout During Initialization:**

```java
// 7-day attribution window (604800 seconds)
InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", false, false, 604800);
```

**Check Attribution Validity:**

```java
boolean isValid = InsertAffiliateManager.isAffiliateAttributionValid(this);
if (isValid) {
    // Attribution is still active
} else {
    // Attribution expired
}
```

**Common Timeout Values:**
- 1 day: `86400`
- 7 days: `604800` (recommended)
- 30 days: `2592000`
- No timeout: `0` (default)

**Get Attribution Date:**

```java
long storedDate = InsertAffiliateManager.getAffiliateStoredDate(this);
// Returns seconds since epoch
```

</details>


---

## 🔍 Troubleshooting

### Initialization Issues

**Error:** "Company code is not set"
- **Cause:** SDK not initialized or `init()` called after other SDK methods
- **Solution:** Call `InsertAffiliateManager.init()` in `onCreate()` before any other SDK methods

### Deep Linking Issues

**Problem:** Deep link opens Play Store or browser instead of app
- **Cause:** Missing or incorrect intent filters, or SHA-256 certificate not configured
- **Solution:**
  - Verify intent filters in `AndroidManifest.xml` match your deep link domain
  - Add SHA-256 certificate fingerprint to your deep linking provider's console
  - For Play App Signing, use SHA-256 from **Google Play Console → Setup → App Integrity**

**Problem:** "No affiliate identifier found"
- **Cause:** User hasn't clicked an affiliate link yet
- **Solution:** Ensure users come from affiliate links before purchases. Test with `adb` command:
  ```bash
  adb shell am start -a android.intent.action.VIEW -d "YOUR_DEEP_LINK_URL"
  ```

### Purchase Tracking Issues

**Problem:** Purchases not appearing in Insert Affiliate dashboard
- **Cause:** Webhook not configured or affiliate identifier not passed to IAP platform
- **Solution:**
  - Verify webhook URL and authorization headers are correct
  - For RevenueCat: Confirm `insert_affiliate` attribute is set before purchase
  - For Iaptic/Google Direct: Check that affiliate identifier exists when purchase is made
  - Enable verbose logging and check console for errors

### Verbose Logging

Enable detailed logs during development to diagnose issues:

```java
InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", true);
```

Filter logs by tag:

```bash
adb logcat | grep "InsertAffiliate TAG"
```

**Important:** Disable verbose logging in production builds.

### Getting Help

- 📖 [Documentation](https://docs.insertaffiliate.com)
- 💬 [Dashboard Support](https://app.insertaffiliate.com/help)
- 🐛 [Report Issues](https://github.com/Insert-Affiliate/InsertAffiliateAndroidSDK/issues)

---

## 📚 Support

- **Documentation**: [docs.insertaffiliate.com](https://docs.insertaffiliate.com)
- **Dashboard Support**: [app.insertaffiliate.com/help](https://app.insertaffiliate.com/help)
- **Issues**: [GitHub Issues](https://github.com/Insert-Affiliate/InsertAffiliateAndroidSDK/issues)
- **Company Code**: [Get yours from Settings](https://app.insertaffiliate.com/settings)

---

**Need help getting started?** Check out our [quickstart guide](https://docs.insertaffiliate.com) or [contact support](https://app.insertaffiliate.com/help).
