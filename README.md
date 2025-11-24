# Insert Affiliate Android SDK

[![Version](https://jitpack.io/v/Insert-Affiliate/InsertAffiliateAndroidSDK.svg)](https://jitpack.io/#Insert-Affiliate/InsertAffiliateAndroidSDK)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

The official Android SDK for [Insert Affiliate](https://insertaffiliate.com) - track affiliate-driven in-app purchases and reward your partners automatically.

**What does this SDK do?** It connects your Android app to Insert Affiliate's platform, enabling you to track which affiliates drive subscriptions and automatically pay them commissions when users make in-app purchases.

## 📋 Table of Contents

- [Quick Start (5 Minutes)](#-quick-start-5-minutes)
- [Essential Setup](#-essential-setup)
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
| [**Google Play Direct (Beta)**](#option-2-google-play-direct-beta) | Cost-focused users, no 3rd party fees | ~15 min | ⭐⭐ Medium |
| [**Iaptic**](#option-3-iaptic) | Custom requirements, direct control | ~20 min | ⭐⭐⭐ Advanced |

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
<summary><h4>Option 2: Google Play Direct (Beta)</h4></summary>

Our direct Google Play integration is currently in beta.

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
<summary><h4>Option 3: Iaptic</h4></summary>

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
                    Log.i("InsertAffiliate", "Affiliate identifier: " + identifier);

                    // If using RevenueCat, update attributes here
                    if (identifier != null) {
                        Map<String, String> attributes = new HashMap<>();
                        attributes.put("insert_affiliate", identifier);
                        Purchases.getSharedInstance().setAttributes(attributes);
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

        // Handle deep link when app is already running
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

**Prerequisites:**
- [Branch SDK for Android](https://help.branch.io/developers-hub/docs/android-basic-integration) installed and configured
- Create a Branch deep link and provide it to affiliates via the [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates)

**Code Implementation:**

```java
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize Branch and capture referring link
        Branch.sessionBuilder(this).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject,
                                     LinkProperties linkProperties,
                                     BranchError error) {
                if (error == null && branchUniversalObject != null) {
                    try {
                        // Extract referring link from Branch
                        String referringLink = branchUniversalObject
                            .getContentMetadata()
                            .convertToJson()
                            .getString("~referring_link");

                        // Pass to Insert Affiliate SDK
                        InsertAffiliateManager.setInsertAffiliateIdentifier(
                            MainActivity.this,
                            referringLink
                        );

                        // If using RevenueCat
                        String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(MainActivity.this);
                        if (affiliateId != null) {
                            Map<String, String> attributes = new HashMap<>();
                            attributes.put("insert_affiliate", affiliateId);
                            Purchases.getSharedInstance().setAttributes(attributes);
                        }
                    } catch (JSONException e) {
                        Log.e("Branch", "Error parsing Branch data: " + e.getMessage());
                    }
                }
            }
        }).withData(this.getIntent().getData()).init();
    }
}
```

**Expected Console Output:**
```
I/InsertAffiliate TAG: [Insert Affiliate] Setting affiliate identifier.
I/InsertAffiliate TAG: [Insert Affiliate] Referring link saved successfully: https://branch.io/abc123
I/InsertAffiliate TAG: [Insert Affiliate] Short link received: AFFILIATE1
```

✅ **Branch.io setup complete!** Skip to [Verify Your Integration](#-verify-your-integration)

</details>

<details>
<summary><h4>Option 3: AppsFlyer</h4></summary>

**Prerequisites:**
- [AppsFlyer SDK for Android](https://dev.appsflyer.com/hc/docs/android-sdk-reference-getting-started) installed and configured
- Create an AppsFlyer OneLink and provide it to affiliates via the [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates)

**Code Implementation:**

```java
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerConversionListener;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InsertAffiliateManager.init(this, "YOUR_COMPANY_CODE", true);

        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onAppOpenAttribution(Map<String, String> attributionData) {
                // Extract deep link from AppsFlyer
                String link = attributionData.get("af_dp");
                if (link == null) link = attributionData.get("af_deeplink");
                if (link == null) link = attributionData.get("link");

                if (link != null) {
                    InsertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, link);
                }
            }

            // Other callback methods...
        };

        AppsFlyerLib.getInstance().registerConversionListener(this, conversionListener);
    }
}
```

✅ **AppsFlyer setup complete!** Proceed to [Verify Your Integration](#-verify-your-integration)

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
<summary><h3>Short Codes (Beta)</h3></summary>

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

**Get Stored Offer Code:**

```java
String offerCode = InsertAffiliateManager.getStoredOfferCode(this);
if (offerCode != null) {
    // Construct dynamic product ID
    String productId = "oneMonthSubscription" + offerCode; // e.g., "oneMonthSubscription-oneweekfree"
    // Use this productId with Google Play Billing or RevenueCat
}
```

**Example with Google Play Billing:**

```java
String baseProductId = "premium_monthly";
String offerCode = InsertAffiliateManager.getStoredOfferCode(this);

List<String> skuList = new ArrayList<>();
if (offerCode != null) {
    skuList.add(baseProductId + offerCode); // Try promotional product first
}
skuList.add(baseProductId); // Fallback to base product

// Query SKUs and launch billing flow
```

See the [full dynamic offer codes guide](#dynamic-offer-codes-complete-guide) below for complete examples.

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


<details id="dynamic-offer-codes-complete-guide">
<summary><h3>Dynamic Offer Codes Complete Guide</h3></summary>

### Setup in Insert Affiliate Dashboard

1. Go to [app.insertaffiliate.com/affiliates](https://app.insertaffiliate.com/affiliates)
2. Select an affiliate and click "View"
3. Set the **Android IAP Modifier** (e.g., `-oneweekfree`)
4. Save settings

### Setup in Google Play Console

**Option 1: Multiple Products (Recommended for Simplicity)**

Create two products:
- Base: `oneMonthSubscription`
- Promo: `oneMonthSubscription-oneweekfree`

Both must be activated and published to at least Internal Testing.

**Option 2: Single Product with Multiple Base Plans**

Create one product with multiple base plans, one with an offer attached.

**Option 3: Developer Triggered Offers**

Use one base product and apply offers programmatically.

### Example: Google Play Billing Integration

```java
import com.android.billingclient.api.*;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class SubscriptionManager {
    private static final String BASE_PRODUCT_ID = "oneMonthSubscription";
    private BillingClient billingClient;
    private Activity activity;

    public void fetchProducts() {
        String offerCode = InsertAffiliateManager.getStoredOfferCode(activity);

        List<String> skuList = new ArrayList<>();
        if (offerCode != null) {
            skuList.add(BASE_PRODUCT_ID + offerCode); // Promotional product
        }
        skuList.add(BASE_PRODUCT_ID); // Fallback

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.SUBS)
            .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Sort to prioritize promotional product
                if (offerCode != null) {
                    String promoProductId = BASE_PRODUCT_ID + offerCode;
                    skuDetailsList.sort((a, b) ->
                        a.getSku().equals(promoProductId) ? -1 : 1);
                }

                // Launch billing flow with first product (promotional if available)
                launchBillingFlow(skuDetailsList.get(0));
            }
        });
    }
}
```

### Example: RevenueCat Integration

**Setup in RevenueCat Dashboard:**

Create separate offerings:
- `premium_monthly` (base offering)
- `premium_monthly_oneweekfree` (promotional offering)

**Code:**

```java
import com.revenuecat.purchases.*;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class RevenueCatSubscriptionManager {
    private static final String BASE_OFFERING = "premium_monthly";

    public void fetchOfferings() {
        String offerCode = InsertAffiliateManager.getStoredOfferCode(activity);

        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                Offering targetOffering = null;

                if (offerCode != null) {
                    // Try promotional offering first
                    String promoKey = BASE_OFFERING + offerCode;
                    targetOffering = offerings.get(promoKey);
                }

                // Fallback to base offering
                if (targetOffering == null) {
                    targetOffering = offerings.get(BASE_OFFERING);
                }

                if (targetOffering != null) {
                    Package packageToPurchase = targetOffering.getAvailablePackages().get(0);
                    purchasePackage(packageToPurchase);
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e("RevenueCat", "Error: " + error.getMessage());
            }
        });
    }
}
```

### Expected Console Output

```
I/InsertAffiliate TAG: [Insert Affiliate] Attempting to fetch offer code for stored affiliate identifier...
I/InsertAffiliate TAG: [Insert Affiliate] Successfully fetched and cleaned offer code: -oneweekfree
I/InsertAffiliate TAG: [Insert Affiliate] Successfully stored offer code: -oneweekfree
I/InsertAffiliate TAG: [Insert Affiliate] Offer code retrieved and stored successfully
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
