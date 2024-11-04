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

    public InsertAffiliateManager(Context context) {
        this.context = context;
    }

    public static void trackEvent(Activity activity, String eventName) {
        String deepLinkParam = getUniqueId(activity);
        if (deepLinkParam == null) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("eventName", eventName);
            payload.put("deepLinkParam", deepLinkParam);
            byte[] jsonData = payload.toString().getBytes(StandardCharsets.UTF_8);

            String apiUrlString = "https://api.insertaffiliate.com/v1/trackEvent";
            URL apiUrl = new URL(apiUrlString);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonData);
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Log.i("InsertAffiliate TAG", "[Insert Affiliate] Event tracked successfully");
            } else {
                Log.i("InsertAffiliate TAG", "[Insert Affiliate] Failed to track event with status code: " + responseCode);
            }
        } catch (Exception e) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] Error tracking event: " + e.getMessage());
        }
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
