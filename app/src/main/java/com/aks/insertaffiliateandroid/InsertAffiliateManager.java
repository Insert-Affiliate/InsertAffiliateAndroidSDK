package com.aks.insertaffiliateandroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;

import java.security.SecureRandom;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class InsertAffiliateManager {
    private ReceiptVerificationCredentials receiptVerificationCredentials;
    private String message = null;
    private Context context;

    public InsertAffiliateManager(Context context, ReceiptVerificationCredentials credentials) {
        this.receiptVerificationCredentials = credentials;
    }

    public InsertAffiliateManager(Context context) {
        this.context = context;
    }

    // TODO: can we make this private function and add our own on init call for this when the package is init so that the user just has to add to app delegate or something a call to us?
    private static void saveUniqueInsertAffiliateId(Activity activity) {
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

    public static String getReflink(Activity activity) {
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );
        String uniqueId = sharedPreferences.getString("deviceid", "");
        String referring_link = sharedPreferences.getString("referring_link", "");
        return referring_link + "/" + uniqueId;
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
        editor.putString("referring_link", reflink);
        editor.commit();
    }

    private boolean hasReceiptVerificationCredentials() {
        return this.receiptVerificationCredentials != null;
    }

    public void init(Activity activity) {
        saveUniqueInsertAffiliateId(activity);
    }

    public String callApiForValidate(
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
            String baseauth = Base64.encodeToString(yourIapticAuthHeader.getBytes(), Base64.NO_WRAP);
            Call<JsonObject> call = api.validaterec(jsonParams, "Basic " + baseauth);
            call.enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Log.i("InsertAffiliate TAG", "Receipt Validated Successfully");
                    message = "Success";
                }

                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.i("InsertAffiliate TAG", "Error While Validating Receipt");
                    message = "Error";
                }
            });
        } else {
            message = "Please provide app name and secret key from iaptic";
            // TODO: notify user that they have not initialised our package with the receipt verificaiton credentials required. Perhaps the package should REQUIRE these params to work otherwise throw an error
        }
        return message;
    }
}
