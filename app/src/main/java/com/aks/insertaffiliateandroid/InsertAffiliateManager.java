package com.aks.insertaffiliateandroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private static String companyCode;
    private String message = null;
    private static String responseMessage = null;

    public InsertAffiliateManager(Context context) {
        this.context = context;
    }

    // MARK: Company Code
    public static void init(Activity activity, String code){
        if (companyCode != null || code == null || code.isEmpty()) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] SDK is already initialized with a company code that isn't null.");
        }
        companyCode = code;
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] SDK initialized with company code: " + companyCode);
        storeAndReturnShortUniqueDeviceId(activity); // Saving device UUID
    }

    public static String getCompanyCode() {
        return companyCode;
    }

    public static void reset() {
        companyCode = null;
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] SDK has been reset.");
    }

    // MARK: Short Codes
    public static boolean isShortCode(String link) {
        // Check if the link is 10 characters long and contains only letters and numbers
        String regex = "^[a-zA-Z0-9]{10}$";
        return link != null && link.matches(regex);
    }

    public static void setShortCode(Activity activity, String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: Short code cannot be null or empty.");
            return;
        }

        // Convert short code to uppercase
        String capitalisedShortCode = shortCode.toUpperCase();

        // Ensure the short code is exactly 10 characters
        if (capitalisedShortCode.length() != 10) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: Short code must be exactly 10 characters long.");
            return;
        }

        // Ensure the short code contains only letters and numbers
        if (!capitalisedShortCode.matches("^[a-zA-Z0-9]+$")) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: Short code must contain only letters and numbers.");
            return;
        }

        // If all checks pass, set the Insert Affiliate Identifier
        storeInsertAffiliateReferringLink(activity,capitalisedShortCode);

        // Return and log the Insert Affiliate Identifier
        String identifier = returnInsertAffiliateIdentifier(activity);
        if (identifier != null) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] Successfully set affiliate identifier: " + identifier);
        } else {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to set affiliate identifier.");
        }
    }

    // MARK: Device UUID
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

    private static String returnShortUniqueDeviceId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        return sharedPreferences.getString("shortUniqueDeviceID", null);
    }

    private static String storeAndReturnShortUniqueDeviceId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        String savedAndroidId = sharedPreferences.getString("shortUniqueDeviceID", null);

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
            editor.putString("shortUniqueDeviceID", shortUniqueId);
            editor.apply();
        }

        return sharedPreferences.getString("shortUniqueDeviceID", null);
    }

    public static String getUniqueId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        return sharedPreferences.getString("shortUniqueDeviceID", null);
    }

    public static void storeExpectedPlayStoreTransaction(Activity activity, String purchaseToken) {
        String companyCode = getCompanyCode();
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Company code is not set. Please initialise the SDK with a valid company code.");
            return;
        }
    
        String shortCode = returnInsertAffiliateIdentifier(activity);
        if (shortCode == null || shortCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            return;
        }
    
        // Build JSON payload
        JSONObject payload = new JSONObject();
        try {
            payload.put("UUID", purchaseToken);
            payload.put("companyCode", companyCode);
            payload.put("shortCode", shortCode);
            payload.put("storedDate", java.time.Instant.now().toString());  // ISO8601 date
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to build JSON payload: " + e.getMessage());
            return;
        }

        String apiUrl = "https://api.insertaffiliate.com/v1/api/app-store-webhook/create-expected-transaction";

        // Networking done on background thread
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
    
                // Write JSON payload
                byte[] outputBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(outputBytes);
    
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Expected transaction stored successfully.");
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to store expected transaction with status code: " + responseCode + ". Response: " + response);
                }
            } catch (Exception e) {
                Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error storing expected transaction: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // MARK: Setting Insert Affiliate Link
    public static void setInsertAffiliateIdentifier(Activity activity, String referringLink) {
        // Check if the companyCode is set
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Company code is not set. Please initialize the SDK with a valid company code.");
            return;
        }

        // Check if the link is already a short code
        if (isShortCode(referringLink)) {
            Log.e("InsertAffiliate TAG","[Insert Affiliate] Referring link is already a short code");
            storeInsertAffiliateReferringLink(activity, referringLink);
            return;
        }

        // Encoding the long form referring link before using it to try and get the Short Link from our API
        String encodedAffiliateLink;
        try {
            encodedAffiliateLink = URLEncoder.encode(referringLink, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to encode referring link: " + e.getMessage());
            storeInsertAffiliateReferringLink(activity, referringLink);
            return;
        }

        String urlString = "https://api.insertaffiliate.com/V1/convert-deep-link-to-short-link?companyId="
            + companyCode 
            + "&deepLinkUrl=" 
            + encodedAffiliateLink;

        try {
            URL url = new URL(urlString);
            // Perform the GET request in a new thread
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Content-Type", "application/json");
    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
    
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
    
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String shortLink = jsonResponse.optString("shortLink");
    
                        if (!shortLink.isEmpty()) {
                            Log.i("InsertAffiliate TAG", "[Insert Affiliate] Short link received: " + shortLink);
                            storeInsertAffiliateReferringLink(activity, shortLink);
                        } else {
                            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Unexpected JSON format");
                            storeInsertAffiliateReferringLink(activity, referringLink);
                        }
                    } else {
                        Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed with HTTP code: " + responseCode);
                        storeInsertAffiliateReferringLink(activity, referringLink);
                    }
                } catch (Exception e) {
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: " + e.getMessage());
                    storeInsertAffiliateReferringLink(activity, referringLink);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();
        } catch (MalformedURLException e) {
            Log.e("InsertAffiliate TAG", "Invalid URL: " + urlString);
            storeInsertAffiliateReferringLink(activity, referringLink);
        }

        // Log success
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Referring link saved successfully: " + referringLink);
    }

    private static void storeInsertAffiliateReferringLink(Activity activity, String referringLink) {
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = sharedPreferences
                .edit();
        editor.putString("referring_link", referringLink);
        editor.commit();
    }

    public static String returnInsertAffiliateIdentifier(Activity activity) {
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );
        String shortUniqueDeviceID = sharedPreferences.getString("shortUniqueDeviceID", "");
        String referring_link = sharedPreferences.getString("referring_link", "");

        if (referring_link == null || referring_link.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            return null;
        }

        return referring_link + "-" + shortUniqueDeviceID;
    }

    // MARK: Event Tracking
    public static String trackEvent(Activity activity, String eventName) {
        String deepLinkParam = returnInsertAffiliateIdentifier(activity);
        if (deepLinkParam == null) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            return "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events by opening a link from an affiliate.";
        }

        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Company code is not set. Please initialise the SDK with a valid company code.");
            return "[Insert Affiliate] Company code is not set. Please initialise the SDK with a valid company code.";
        }

        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty("eventName", eventName);
        jsonParams.addProperty("companyId", companyCode);

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


    // MARK: Validation with Iaptic API
    public String validatePurchaseWithIapticAPI(
        Activity activity,
        String appname,
        String publicKey,
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
            returnInsertAffiliateIdentifier(activity));

        jsonParams.addProperty("id", subscriptionId);
        jsonParams.addProperty("type", "paid subscription");
        jsonParams.add("transaction", objTrans);
        jsonParams.add("additionalData", objAddData);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL_IAPTIC_VALIDATOR)
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        Api api = retrofit.create(Api.class);

        String yourIapticAuthHeader = appname + ":" + publicKey;
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
