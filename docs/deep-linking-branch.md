# Branch.io Deep Linking Integration

This guide shows how to integrate Insert Affiliate SDK with Branch.io for deep linking attribution.

## Prerequisites

- [Branch SDK for Android](https://help.branch.io/developers-hub/docs/android-basic-integration) installed and configured
- Create a Branch deep link and provide it to affiliates via the [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates)

## Code Implementation

Choose the example that matches your IAP verification platform:

### With RevenueCat

```java
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import com.revenuecat.purchases.Purchases;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Insert Affiliate SDK
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
    }

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
                        String referringLink = branchUniversalObject
                            .getContentMetadata()
                            .convertToJson()
                            .getString("~referring_link");

                        // This triggers the callback which updates RevenueCat
                        InsertAffiliateManager.setInsertAffiliateIdentifier(
                            MainActivity.this,
                            referringLink
                        );
                    } catch (JSONException e) {
                        Log.e("Branch", "Error parsing Branch data: " + e.getMessage());
                    }
                }
            }
        }).withData(this.getIntent().getData()).init();
    }
}
```

### With Adapty

```java
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import com.adapty.Adapty;
import com.adapty.models.AdaptyProfileParameters;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Insert Affiliate SDK
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
                        String referringLink = branchUniversalObject
                            .getContentMetadata()
                            .convertToJson()
                            .getString("~referring_link");

                        // This triggers the callback which updates Adapty
                        InsertAffiliateManager.setInsertAffiliateIdentifier(
                            MainActivity.this,
                            referringLink
                        );
                    } catch (JSONException e) {
                        Log.e("Branch", "Error parsing Branch data: " + e.getMessage());
                    }
                }
            }
        }).withData(this.getIntent().getData()).init();
    }
}
```

### With Google Play Direct or Iaptic

```java
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import com.aks.insertaffiliateandroid.InsertAffiliateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Insert Affiliate SDK
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
    }

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
                        String referringLink = branchUniversalObject
                            .getContentMetadata()
                            .convertToJson()
                            .getString("~referring_link");

                        InsertAffiliateManager.setInsertAffiliateIdentifier(
                            MainActivity.this,
                            referringLink
                        );
                    } catch (JSONException e) {
                        Log.e("Branch", "Error parsing Branch data: " + e.getMessage());
                    }
                }
            }
        }).withData(this.getIntent().getData()).init();
    }
}
```

## Expected Console Output

```
I/InsertAffiliate TAG: [Insert Affiliate] Setting affiliate identifier.
I/InsertAffiliate TAG: [Insert Affiliate] Referring link saved successfully: https://branch.io/abc123
I/InsertAffiliate TAG: [Insert Affiliate] Short link received: AFFILIATE1
```

## Verification

Test your Branch deep link integration:

```bash
# Replace with your actual Branch link
adb shell am start -a android.intent.action.VIEW -d "https://your-app.app.link/abc123"
```

Check that the affiliate identifier is stored:

```java
String affiliateId = InsertAffiliateManager.returnInsertAffiliateIdentifier(this);
Log.d("Test", "Current affiliate ID: " + affiliateId);
```

## Troubleshooting

**Problem:** Deep link not capturing in Branch callback
- **Solution:** Ensure Branch SDK is properly initialized before Insert Affiliate SDK
- Check Branch dashboard for test link status

**Problem:** `~referring_link` is null
- **Solution:** User may have opened app directly instead of via deep link
- Verify Branch link is properly configured with your app's URI scheme

## Next Steps

After completing Branch integration:
1. Test deep link attribution with a test affiliate link
2. Verify affiliate identifier is stored correctly
3. Make a test purchase to confirm tracking works end-to-end

[← Back to Main README](../README.md)
