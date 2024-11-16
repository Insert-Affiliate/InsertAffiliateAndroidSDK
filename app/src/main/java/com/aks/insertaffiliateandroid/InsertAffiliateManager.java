package com.aks.insertaffiliateandroid;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import java.security.SecureRandom;
import java.util.function.Consumer;
import java.net.URLEncoder;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
    private final Context context;
    private String message = null;
    private static String responseMessage = null;

    public InsertAffiliateManager(Context context) {
        this.context = context;
    }

    public static String trackEvent(Activity activity, String eventName) {
        String deepLinkParam = getReflink(activity);
        if (deepLinkParam == null) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            return "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events by opening a link from an affiliate.";
        }

        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty("eventName", eventName);
        
        // URL encode the deepLinkParam if the Android version supports it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            deepLinkParam = URLEncoder.encode(deepLinkParam, StandardCharsets.UTF_8);
        }
        jsonParams.addProperty("deepLinkParam", deepLinkParam);

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(Api.BASE_URL_INSERT_AFFILIATE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(new OkHttpClient.Builder().build())
            .build();

        Api api = retrofit.create(Api.class);

        Call<JsonObject> call = api.trackevent(jsonParams);

        call.enqueue(new Callback<JsonObject>() {
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                int responseCode = response.code();
                Log.d("InsertAffiliate response: ", "" + response.body());

                if (responseCode == 200) {
                    responseMessage = "[Insert Affiliate] Track Event Success";
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Event tracked successfully");
                } else {
                    responseMessage = "[Insert Affiliate] Failed to track event with status code: " + responseCode;
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Failed to track event with status code: " + responseCode);
                }
            }

            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.i("InsertAffiliate TAG", "Error While Validating Receipt");
                responseMessage = "Error";
            }
        });

        return responseMessage;
    }

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

    public void init(Activity activity) {
        saveUniqueInsertAffiliateId(activity);
    }

    public String callApiForValidate(
            Activity activity,
            String appname,
            String secretkey,
            String subscriptionId,
            String purchaseId,
            String purchaseToken,
            String receipt,
            String signature
    ) {

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
                .baseUrl(Api.BASE_URL)
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        Api api = retrofit.create(Api.class);

        String yourIapticAuthHeader = appname + ":" + secretkey;
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

        return message;
    }
}
