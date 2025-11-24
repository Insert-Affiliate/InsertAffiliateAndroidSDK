# Branch.io Deep Linking Integration

This guide shows how to integrate Insert Affiliate SDK with Branch.io for deep linking attribution.

## Prerequisites

- [Branch SDK for Android](https://help.branch.io/developers-hub/docs/android-basic-integration) installed and configured
- Create a Branch deep link and provide it to affiliates via the [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates)

## Code Implementation

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
