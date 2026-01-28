# AppsFlyer Deep Linking Integration

This guide shows how to integrate Insert Affiliate SDK with AppsFlyer for deep linking attribution.

## Prerequisites

- [AppsFlyer SDK for Android](https://dev.appsflyer.com/hc/docs/android-sdk-reference-getting-started) installed and configured
- Create an AppsFlyer OneLink and provide it to affiliates via the [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates)

## Code Implementation

Choose the example that matches your IAP verification platform:

### With RevenueCat

```java
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerConversionListener;
import com.revenuecat.purchases.Purchases;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",
            true   // Verbose logging
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

        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onAppOpenAttribution(Map<String, String> attributionData) {
                String link = attributionData.get("af_dp");
                if (link == null) link = attributionData.get("af_deeplink");
                if (link == null) link = attributionData.get("link");

                if (link != null) {
                    // This triggers the callback which updates RevenueCat
                    InsertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, link);
                }
            }

            @Override
            public void onConversionDataSuccess(Map<String, Object> conversionData) { }

            @Override
            public void onConversionDataFail(String errorMessage) {
                Log.e("AppsFlyer", "Conversion data failed: " + errorMessage);
            }

            @Override
            public void onAppOpenAttributionFailure(String errorMessage) {
                Log.e("AppsFlyer", "Attribution failed: " + errorMessage);
            }
        };

        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_DEV_KEY", conversionListener, this);
        AppsFlyerLib.getInstance().start(this);
    }
}
```

### With Adapty

```java
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerConversionListener;
import com.adapty.Adapty;
import com.adapty.models.AdaptyProfileParameters;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",
            true   // Verbose logging
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

        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onAppOpenAttribution(Map<String, String> attributionData) {
                String link = attributionData.get("af_dp");
                if (link == null) link = attributionData.get("af_deeplink");
                if (link == null) link = attributionData.get("link");

                if (link != null) {
                    // This triggers the callback which updates Adapty
                    InsertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, link);
                }
            }

            @Override
            public void onConversionDataSuccess(Map<String, Object> conversionData) { }

            @Override
            public void onConversionDataFail(String errorMessage) {
                Log.e("AppsFlyer", "Conversion data failed: " + errorMessage);
            }

            @Override
            public void onAppOpenAttributionFailure(String errorMessage) {
                Log.e("AppsFlyer", "Attribution failed: " + errorMessage);
            }
        };

        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_DEV_KEY", conversionListener, this);
        AppsFlyerLib.getInstance().start(this);
    }

    private void updateAdaptyProfile(String affiliateId) {
        // Run on background thread to avoid blocking UI while waiting for Adapty
        new Thread(() -> {
            // Wait for Adapty activation before updating profile (see README for MyApp setup)
            MyApp.waitForAdaptyActivation();

            AdaptyProfileParameters.Builder builder = new AdaptyProfileParameters.Builder()
                    .withCustomAttribute("insert_affiliate", affiliateId);

            Adapty.updateProfile(builder.build(), error -> {
                if (error != null) {
                    Log.e("MainActivity", "Failed to update Adapty: " + error.getMessage());
                } else {
                    Log.d("MainActivity", "Adapty updated with: " + affiliateId);
                }
            });
        }).start();
    }
}
```

### With Google Play Direct or Iaptic

```java
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerConversionListener;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InsertAffiliateManager.init(
            this,
            "YOUR_COMPANY_CODE",
            true   // Verbose logging
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

        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
            @Override
            public void onAppOpenAttribution(Map<String, String> attributionData) {
                String link = attributionData.get("af_dp");
                if (link == null) link = attributionData.get("af_deeplink");
                if (link == null) link = attributionData.get("link");

                if (link != null) {
                    InsertAffiliateManager.setInsertAffiliateIdentifier(MainActivity.this, link);
                }
            }

            @Override
            public void onConversionDataSuccess(Map<String, Object> conversionData) { }

            @Override
            public void onConversionDataFail(String errorMessage) {
                Log.e("AppsFlyer", "Conversion data failed: " + errorMessage);
            }

            @Override
            public void onAppOpenAttributionFailure(String errorMessage) {
                Log.e("AppsFlyer", "Attribution failed: " + errorMessage);
            }
        };

        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_DEV_KEY", conversionListener, this);
        AppsFlyerLib.getInstance().start(this);
    }
}
```

## Expected Console Output

```
I/InsertAffiliate TAG: [Insert Affiliate] Setting affiliate identifier.
I/InsertAffiliate TAG: [Insert Affiliate] Referring link saved successfully: https://your-app.onelink.me/abc123
I/InsertAffiliate TAG: [Insert Affiliate] Short link received: AFFILIATE1
```

## Verification

Test your AppsFlyer deep link integration:

```bash
# Replace with your actual OneLink URL
adb shell am start -a android.intent.action.VIEW -d "https://your-app.onelink.me/abc123"
```

Check that the affiliate identifier is stored:

```java
String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(this);
Log.d("Test", "Current affiliate ID: " + affiliateId);
```

## Troubleshooting

**Problem:** Attribution callback not firing
- **Solution:** Ensure AppsFlyer SDK is initialized before creating the conversion listener
- Check AppsFlyer dashboard to verify OneLink is active

**Problem:** Deep link parameters not captured
- **Solution:** Verify the deep link contains the correct parameters in AppsFlyer dashboard
- Ensure your AndroidManifest.xml has correct intent filters for your OneLink domain

**Problem:** Attribution data is empty
- **Solution:** User may have opened app directly instead of via deep link
- Test with a fresh install using the deep link

## Next Steps

After completing AppsFlyer integration:
1. Test deep link attribution with a test affiliate link
2. Verify affiliate identifier is stored correctly
3. Make a test purchase to confirm tracking works end-to-end

[← Back to Main README](../README.md)
