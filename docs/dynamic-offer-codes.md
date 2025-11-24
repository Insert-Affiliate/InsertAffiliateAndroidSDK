# Dynamic Offer Codes Complete Guide

Automatically apply discounts or trials when users come from specific affiliates using offer code modifiers.

## How It Works

1. Configure an offer code modifier in your [Insert Affiliate dashboard](https://app.insertaffiliate.com/affiliates) (e.g., `-oneweekfree`)
2. SDK automatically fetches and stores the modifier when affiliate identifier is set
3. Use the modifier to construct dynamic product IDs

## Setup in Insert Affiliate Dashboard

1. Go to [app.insertaffiliate.com/affiliates](https://app.insertaffiliate.com/affiliates)
2. Select an affiliate and click "View"
3. Set the **Android IAP Modifier** (e.g., `-oneweekfree`)
4. Save settings

## Setup in Google Play Console

Choose one of the following approaches:

### Option 1: Multiple Products (Recommended for Simplicity)

Create two products:
- Base: `oneMonthSubscription`
- Promo: `oneMonthSubscription-oneweekfree`

Both must be activated and published to at least Internal Testing.

### Option 2: Single Product with Multiple Base Plans

Create one product with multiple base plans, one with an offer attached.

### Option 3: Developer Triggered Offers

Use one base product and apply offers programmatically.

## Basic Usage

Get the stored offer code modifier:

```java
String offerCode = InsertAffiliateManager.getStoredOfferCode(this);
if (offerCode != null) {
    // Construct dynamic product ID
    String productId = "oneMonthSubscription" + offerCode;
    // Result: "oneMonthSubscription-oneweekfree"

    // Use this productId with Google Play Billing or RevenueCat
}
```

## Example: Google Play Billing Integration

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

    private void launchBillingFlow(SkuDetails skuDetails) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build();
        billingClient.launchBillingFlow(activity, flowParams);
    }
}
```

## Example: RevenueCat Integration

### Setup in RevenueCat Dashboard

Create separate offerings:
- `premium_monthly` (base offering)
- `premium_monthly_oneweekfree` (promotional offering)

### Code Implementation

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

    private void purchasePackage(Package packageToPurchase) {
        Purchases.getSharedInstance().purchase(
            activity,
            packageToPurchase,
            new PurchaseCallback() {
                @Override
                public void onCompleted(@NonNull Purchase purchase,
                                      @NonNull CustomerInfo customerInfo) {
                    // Purchase successful
                }

                @Override
                public void onError(@NonNull PurchasesError error,
                                   boolean userCancelled) {
                    // Handle error
                }
            }
        );
    }
}
```

## Expected Console Output

When the SDK fetches and stores an offer code:

```
I/InsertAffiliate TAG: [Insert Affiliate] Attempting to fetch offer code for stored affiliate identifier...
I/InsertAffiliate TAG: [Insert Affiliate] Successfully fetched and cleaned offer code: -oneweekfree
I/InsertAffiliate TAG: [Insert Affiliate] Successfully stored offer code: -oneweekfree
I/InsertAffiliate TAG: [Insert Affiliate] Offer code retrieved and stored successfully
```

## Testing

Test the offer code flow:

1. **Click test affiliate link** with offer code modifier configured
2. **Check stored offer code**:
   ```java
   String offerCode = InsertAffiliateManager.getStoredOfferCode(this);
   Log.d("Test", "Stored offer code: " + offerCode);
   ```
3. **Verify product selection** shows promotional product/offering
4. **Complete test purchase** and verify correct product is purchased

## Troubleshooting

**Problem:** Offer code is null
- **Solution:** Ensure affiliate has offer code modifier configured in dashboard
- Verify user clicked affiliate link before checking offer code

**Problem:** Promotional product not found
- **Solution:** Verify promotional product exists in Google Play Console or RevenueCat
- Check product ID matches exactly (including the modifier)

**Problem:** Always showing base product instead of promotional
- **Solution:** Ensure offer code is retrieved before querying products
- Check sort logic prioritizes promotional product correctly

## Best Practices

1. **Always provide a fallback**: If promotional product isn't available, fall back to base product
2. **Test thoroughly**: Verify promotional products are properly configured before going live
3. **Monitor conversion**: Track which affiliates drive promotional vs. regular purchases
4. **Clear expiration**: Consider adding attribution timeout to limit offer code validity

## Next Steps

- Configure offer code modifiers for high-value affiliates
- Create promotional products/offerings in your IAP platform
- Test the complete flow from link click to purchase
- Monitor affiliate performance in Insert Affiliate dashboard

[← Back to Main README](../README.md)
