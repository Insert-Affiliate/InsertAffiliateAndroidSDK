package com.aks.insertaffiliateandroid;

import android.content.SharedPreferences;

import android.app.Activity;
import android.content.Context;
import java.security.SecureRandom;
import android.util.Base64;
import android.provider.Settings;
import com.google.gson.JsonObject;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InsertAffiliateManager {
    private BillingClient billingClient;
    private ReceiptVerificationCredentials receiptVerificationCredentials;

    public InsertAffiliateManager(Context context, ReceiptVerificationCredentials credentials) {
        this.receiptVerificationCredentials = credentials != null ? credentials : null;

        // TODO: Initialize BillingClient and start listening for purchases the user may have triggered within their app... I'd rather us do this work and listen for a purchase than ask the user to - is this possible?
        billingClient = BillingClient.newBuilder(context)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (Purchase purchase : purchases) {
                                // Call verifyPurchase automatically
                                verifyPurchase((Activity) context, getSubscriptionId(purchase), purchase);
                            }
                        }
                    }
                })
                .enablePendingPurchases() // Required for subscriptions
                .build();

        // TODO: Start connection to the billing service -- is this required if the user has already done this themselves separately?
//        billingClient.startConnection(new BillingClientStateListener() {
//            @Override
//            public void onBillingSetupFinished(BillingResult billingResult) {
//                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                    // The BillingClient is ready. You can query purchases here.
//                }
//            }
//
//            @Override
//            public void onBillingServiceDisconnected() {
//                // Try to restart the connection on the next request to Google Play by calling the startConnection() method.
//            }
//        });
    }

    private String getSubscriptionId(Purchase purchase) {
        // Extract the subscription ID from the purchase details
        // TODO: is this a legitemate way to get the subscription / purchase Id of the transaction? Please fix if nott
        return purchase.getProducts().isEmpty() ? null : purchase.getProducts().get(0); // Assuming the first product is the subscriptio
    }

    private boolean hasReceiptVerificationCredentials() {
        return this.receiptVerificationCredentials != null;
    }

    // TODO: All of the rest of the stuff should be handled automatically (Saving id etc). If so, can we make this function private?
    public void verifyPurchase(Activity activity, String subscriptionId, Purchase purchase) { // Call this on purchase...This should be only touch point of user, they should have a simple onboarding process now
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Call your API for validation
                callApiForValidate(
                        activity,
                        subscriptionId,
                        purchase.getOrderId(),
                        purchase.getPurchaseToken(),
                        purchase.getOriginalJson(),
                        purchase.getSignature());
            }
        });
    }

    // TODO: can we make this private function and add our own on init call for this when the package is init so that the user just has to add to app delegate or something a call to us?
    public static void saveUniqueInsertAffiliateId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        String savedAndroidId = sharedPreferences.getString("deviceid", null);

        if (savedAndroidId == null) {
            // Get ANDROID_ID
            String androidId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);

            // If ANDROID_ID is null, generate a random string
            if (androidId == null) {
                androidId = generateRandomString(6);
            }

            // Save trimmed or original ID
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String shortUniqueId = androidId.length() > 6 ? androidId.substring(0, 6) : androidId;
            editor.putString("deviceid", shortUniqueId);
            editor.apply();
        }
    }

    // Method to generate a random alphanumeric string of specified length
    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    private static String getReflink(Activity activity) {
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );
        String uniqueId = sharedPreferences.getString("deviceid", "");
        String referring_link = sharedPreferences.getString("referring_link", "");
        return referring_link + "/" + uniqueId;
    }

    private void callApiForValidate(
                Activity activity,
                String subscriptionId, // TODO: Retrieve & pass this from the purchase, don't pass in strings.xml (delete todo after completion)
                String purchaseId,
                String purchaseToken,
                String receipt,
                String signature
    ) {
        if (hasReceiptVerificationCredentials()) {
            JsonObject jsonParams = new JsonObject();
            JsonObject objTrans = new JsonObject();
            JsonObject objAddData = new JsonObject();

            objTrans.addProperty("type", "android-playstore");
            objTrans.addProperty("id", purchaseId);
            objTrans.addProperty("purchaseToken", purchaseToken);
            objTrans.addProperty("receipt", receipt);
            objTrans.addProperty("signature", signature);

            objAddData.addProperty("applicationUsername",
                    getReflink(activity));

            jsonParams.addProperty("id", subscriptionId);
            jsonParams.addProperty("type", "paid subscription");
            jsonParams.add("transaction", objTrans);
            jsonParams.add("additionalData", objAddData);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Api.BASE_URL) // TODO: what is this and why is it stored in an xml? Doesn't this need to point to Iaptics servers, not ours?
                    .client(new OkHttpClient.Builder().build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();

            Api api = retrofit.create(Api.class);

            String yourIapticAuthHeader = receiptVerificationCredentials.getAppName() + ":" + receiptVerificationCredentials.getSecretKey();
            String baseauth = android.util.Base64.encodeToString(yourIapticAuthHeader.getBytes(), Base64.NO_WRAP);
            Call<JsonObject> call = api.validaterec(jsonParams, "Basic " + baseauth);
            call.enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    // TODO: ...??
                    // TODO: should I be doing something here...? Can you handle this please, maybe some console logs showing success
                }

                public void onFailure(Call<JsonObject> call, Throwable t) {
                    // TODO: ...??
                    // TODO: please handle the on error case with some form of console logging like console.log("[Insert Affiliate Error] -- Error message")
                }
            });
        } else {
            // TODO: notify user that they have not initialised our package with the receipt verificaiton credentials required. Perhaps the package should REQUIRE these params to work otherwise throw an error
        }

    }

    // TODO: Note - these methods probably have to stay public as I don't want to import the Branch.io SDK into our app due to separation of concerns (delete comment once you have taken note of this)
    public static String getUniqueId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        return sharedPreferences.getString("deviceid", null);
    }

    public static void saveReferLink(Activity activity, String reflink) {
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = sharedPreferences
                .edit();
        editor.putString("referring_link", ""+reflink);
        editor.commit();
    }
}
