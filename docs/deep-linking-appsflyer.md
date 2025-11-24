# AppsFlyer Deep Linking Integration

This guide shows how to integrate Insert Affiliate SDK with AppsFlyer for deep linking attribution.

## Prerequisites

- [AppsFlyer SDK for Android](https://dev.appsflyer.com/hc/docs/android-sdk-reference-getting-started) installed and configured
- Create an AppsFlyer OneLink and provide it to affiliates via the [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates)

## Code Implementation

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

                    // If using RevenueCat
                    String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(MainActivity.this);
                    if (affiliateId != null) {
                        Map<String, String> attributes = new HashMap<>();
                        attributes.put("insert_affiliate", affiliateId);
                        Purchases.getSharedInstance().setAttributes(attributes);
                    }
                }
            }

            @Override
            public void onConversionDataSuccess(Map<String, Object> conversionData) {
                // Handle conversion data if needed
            }

            @Override
            public void onConversionDataFail(String errorMessage) {
                Log.e("AppsFlyer", "Conversion data failed: " + errorMessage);
            }

            @Override
            public void onAppOpenAttributionFailure(String errorMessage) {
                Log.e("AppsFlyer", "Attribution failed: " + errorMessage);
            }
        };

        AppsFlyerLib.getInstance().init(
            "YOUR_APPSFLYER_DEV_KEY",
            conversionListener,
            this
        );
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
