package com.aks.insertaffiliateandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static boolean verboseLogging = false;
    private static boolean insertLinks = false;
    private static long affiliateAttributionActiveTime = 0; // Time in seconds for affiliate attribution to remain active (0 = no timeout)
    
    // Thread-safe callback mechanism
    private static final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private static volatile InsertAffiliateIdentifierChangeCallback identifierChangeCallback;

    public InsertAffiliateManager(Context context) {
        this.context = context;
    }

    // MARK: init with default
    public static void init(Activity activity, String code){
        init(activity, code, false, false);
    }
    
    // MARK: init with timeout
    public static void init(Activity activity, String code, long affiliateAttributionActiveTimeSeconds){
        init(activity, code, false, false, affiliateAttributionActiveTimeSeconds);
    }
    
    // MARK: init with logging and timeout
    public static void init(Activity activity, String code, boolean enableVerboseLogging, long affiliateAttributionActiveTimeSeconds){
        init(activity, code, enableVerboseLogging, false, affiliateAttributionActiveTimeSeconds);
    }
    
    public static void init(
        Activity activity,
        String code,
        boolean enableVerboseLogging,
        boolean enableInsertLinks // When set to true, the SDK will add trigger additional setup for deep links and universal links. If you are using an external provider for deep links, ensure this is set to false.

    ){
        init(activity, code, enableVerboseLogging, enableInsertLinks, 0);
    }
    
    public static void init(
        Activity activity,
        String code,
        boolean enableVerboseLogging,
        boolean enableInsertLinks, // When set to true, the SDK will add trigger additional setup for deep links and universal links. If you are using an external provider for deep links, ensure this is set to false.
        long affiliateAttributionActiveTimeSeconds // Time in seconds for affiliate attribution to remain active (0 = no timeout)
    ){
        verboseLogging = enableVerboseLogging;
        insertLinks = enableInsertLinks;
        affiliateAttributionActiveTime = affiliateAttributionActiveTimeSeconds;

        if (verboseLogging) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] Starting SDK initialization...");
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] Company code provided: " + (code != null && !code.isEmpty() ? "Yes" : "No"));
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] Verbose logging enabled");
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] Insert links enabled: " + insertLinks);
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] Affiliate attribution timeout: " + (affiliateAttributionActiveTime > 0 ? affiliateAttributionActiveTime + " seconds" : "disabled"));
        }
        
        if (companyCode != null || code == null || code.isEmpty()) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] SDK is already initialized with a company code that isn't null.");
        }
        companyCode = code;
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] SDK initialized with company code: " + companyCode);
        storeAndReturnShortUniqueDeviceId(activity); // Saving device UUID
        
        if (verboseLogging) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] SDK initialization completed");
        }
        
        // Automatically capture install referrer data if enabled
        if (insertLinks) {
            captureInstallReferrer(activity); // Deferred Deep Linking
        }
    }

    public static String getCompanyCode() {
        return companyCode;
    }

    public static void reset() {
        companyCode = null;
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] SDK has been reset.");
    }

    /**
     * Sets a callback to be notified whenever the affiliate identifier changes
     * @param callback The callback to be invoked when the identifier changes
     */
    public static void setInsertAffiliateIdentifierChangeCallback(InsertAffiliateIdentifierChangeCallback callback) {
        identifierChangeCallback = callback;
        if (verboseLogging) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] Affiliate identifier change callback " + 
                  (callback != null ? "set" : "removed"));
        }
    }

    // MARK: Short Codes
    public static boolean isShortCode(String link) {
        // Check if the link is between 3 and 25 characters long and contains only letters and numbers
        String regex = "^[a-zA-Z0-9]{3,25}$";
        return link != null && link.matches(regex);
    }

    /**
     * Validates a short code against the API and stores it if valid
     * @param activity The activity context
     * @param shortCode The short code to validate and set
     * @param callback Callback that receives validation result (true if valid, false if invalid)
     */
    public static void setShortCode(Activity activity, String shortCode, ShortCodeValidationCallback callback) {
        if (shortCode == null || shortCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: Short code cannot be null or empty.");
            if (callback != null) callback.onValidationComplete(false);
            return;
        }

        // Convert short code to uppercase
        String capitalisedShortCode = shortCode.toUpperCase();

        // Ensure the short code is between 3 and 25 characters
        if (capitalisedShortCode.length() < 3 || capitalisedShortCode.length() > 25) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: Short code must be between 3 and 25 characters long.");
            if (callback != null) callback.onValidationComplete(false);
            return;
        }

        // Ensure the short code contains only letters and numbers
        if (!capitalisedShortCode.matches("^[a-zA-Z0-9]+$")) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error: Short code must contain only letters and numbers.");
            if (callback != null) callback.onValidationComplete(false);
            return;
        }

        // Validate against API
        getAffiliateDetails(capitalisedShortCode, new AffiliateDetailsCallback() {
            @Override
            public void onAffiliateDetailsReceived(AffiliateDetails details) {
                if (details != null) {
                    // Valid short code, store it
                    storeInsertAffiliateReferringLink(activity, capitalisedShortCode);
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Short code " + capitalisedShortCode + " validated and stored successfully.");
                    if (callback != null) callback.onValidationComplete(true);
                } else {
                    // Invalid short code
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Short code " + capitalisedShortCode + " does not exist. Not storing.");
                    if (callback != null) callback.onValidationComplete(false);
                }
            }
        });
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
        verboseLog("Getting or generating user ID...");
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        String savedAndroidId = sharedPreferences.getString("shortUniqueDeviceID", null);

        if (savedAndroidId == null) {
            verboseLog("No existing user ID found, generating new one...");
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
            verboseLog("Generated and saved new user ID: " + shortUniqueId);
        } else {
            verboseLog("Found existing user ID: " + savedAndroidId);
        }

        return sharedPreferences.getString("shortUniqueDeviceID", null);
    }

    public static String getUniqueId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        return sharedPreferences.getString("shortUniqueDeviceID", null);
    }

    public static void storeExpectedPlayStoreTransaction(Activity activity, String purchaseToken) {
        verboseLog("Storing expected store transaction with token: " + purchaseToken);
        
        String companyCode = getCompanyCode();
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Company code is not set. Please initialise the SDK with a valid company code.");
            verboseLog("Cannot store transaction: no company code available");
            return;
        }
    
        String shortCode = returnInsertAffiliateIdentifier(activity);
        if (shortCode == null || shortCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            verboseLog("Cannot store transaction: no affiliate identifier available");
            return;
        }
        
        verboseLog("Company code: " + companyCode + ", Short code: " + shortCode);
    
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
        
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Storing expected transaction: " + payload);
        verboseLog("Making API call to store expected transaction...");

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
                verboseLog("API response status: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Expected transaction stored successfully.");
                    verboseLog("Expected transaction stored successfully on server");
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to store expected transaction with status code: " + responseCode + ". Response: " + response);
                    verboseLog("API error response: " + response);
                }
            } catch (Exception e) {
                Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error storing expected transaction: " + e.getMessage());
                verboseLog("Network error storing transaction: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // MARK: Setting Insert Affiliate Link
    public static void setInsertAffiliateIdentifier(Activity activity, String referringLink) {
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Setting affiliate identifier.");
        verboseLog("Input referringLink: " + referringLink);
        
        // Check if the companyCode is set
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Company code is not set. Please initialize the SDK with a valid company code.");
            verboseLog("Company code missing, cannot proceed with API call");
            return;
        }

        verboseLog("Checking if referring link is already a short code...");
        // Check if the link is already a short code
        if (isShortCode(referringLink)) {
            Log.i("InsertAffiliate TAG","[Insert Affiliate] Referring link is already a short code.");
            verboseLog("Link is already a short code, storing directly");
            storeInsertAffiliateReferringLink(activity, referringLink);
            return;
        }
        
        verboseLog("Link is not a short code, will convert via API");

        verboseLog("Encoding referring link for API call...");
        // Encoding the long form referring link before using it to try and get the Short Link from our API
        String encodedAffiliateLink;
        try {
            encodedAffiliateLink = URLEncoder.encode(referringLink, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to encode referring link: " + e.getMessage());
            verboseLog("Error encoding referring link: " + e.getMessage());
            storeInsertAffiliateReferringLink(activity, referringLink);
            return;
        }

        String urlString = "https://api.insertaffiliate.com/V1/convert-deep-link-to-short-link?companyId="
            + companyCode 
            + "&deepLinkUrl=" 
            + encodedAffiliateLink;

        verboseLog("Making API request to convert deep link to short code...");
        
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
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Storing affiliate identifier: " + referringLink);
        
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );

        // Check if this is a new or different affiliate identifier
        String existingLink = sharedPreferences.getString("referring_link", null);
        boolean isNewOrDifferent = existingLink == null || !existingLink.equals(referringLink);

        SharedPreferences.Editor editor = sharedPreferences
                .edit();
        editor.putString("referring_link", referringLink);
        
        // Only store the attribution date if this is a new or different affiliate identifier
        if (isNewOrDifferent) {
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            editor.putLong("affiliate_stored_date", currentTimeSeconds);
            verboseLog("New affiliate identifier stored with fresh attribution date: " + currentTimeSeconds);
        } else {
            verboseLog("Same affiliate identifier, preserving existing attribution date");
        }
        
        editor.commit();
        
        // Notify callback of identifier change
        notifyIdentifierChange(activity);
        
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Attempting to fetch offer code for stored affiliate identifier...");
        retrieveAndStoreOfferCode(activity, referringLink);
    }

    public static String returnInsertAffiliateIdentifier(Activity activity) {
        return returnInsertAffiliateIdentifier(activity, false);
    }
    
    public static String returnInsertAffiliateIdentifier(Activity activity, boolean ignoreTimeout) {
        verboseLog("Getting insert affiliate identifier (ignoreTimeout: " + ignoreTimeout + ")...");
        SharedPreferences sharedPreferences
                = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE
        );
        String shortUniqueDeviceID = sharedPreferences.getString("shortUniqueDeviceID", "");
        String referring_link = sharedPreferences.getString("referring_link", "");
        
        verboseLog("SharedPreferences - referringLink: " + (referring_link.isEmpty() ? "empty" : referring_link) + ", shortUniqueDeviceID: " + (shortUniqueDeviceID.isEmpty() ? "empty" : shortUniqueDeviceID));

        if (referring_link == null || referring_link.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            verboseLog("No affiliate identifier found in storage");
            return null;
        }
        
        // Check timeout only if not ignoring timeout and timeout is configured
        if (!ignoreTimeout && affiliateAttributionActiveTime > 0) {
            if (!isAffiliateAttributionValid(activity)) {
                Log.i("InsertAffiliate TAG", "[Insert Affiliate] Affiliate attribution has expired");
                verboseLog("Affiliate attribution expired, returning null");
                return null;
            }
        }
        
        String identifier = referring_link + "-" + shortUniqueDeviceID;
        verboseLog("Found identifier: " + identifier);
        return identifier;
    }

    // MARK: Play Install Referrer
    /**
     * Captures install referrer data from Google Play Store
     * This method automatically extracts referral parameters and processes them
     * @param activity The activity context
     */
    private static void captureInstallReferrer(Activity activity) {
        verboseLog("Starting install referrer capture...");
        
        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(activity).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        verboseLog("Install referrer setup successful");
                        try {
                            ReferrerDetails details = referrerClient.getInstallReferrer();
                            String rawReferrer = details.getInstallReferrer();
                            
                            verboseLog("Raw referrer data: " + rawReferrer);
                            
                            if (rawReferrer != null && !rawReferrer.isEmpty()) {
                                processInstallReferrerData(activity, rawReferrer);
                            } else {
                                verboseLog("No referrer data found");
                            }
                        } catch (Exception e) {
                            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error getting install referrer details: " + e.getMessage());
                            verboseLog("Error getting referrer details: " + e.getMessage());
                        }
                        referrerClient.endConnection();
                        break;
                        
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        verboseLog("Install referrer feature not supported on this device");
                        break;
                        
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        verboseLog("Install referrer service unavailable");
                        break;
                        
                    default:
                        verboseLog("Install referrer setup failed with code: " + responseCode);
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                verboseLog("Install referrer service disconnected");
            }
        });
    }

    /**
     * Processes the raw install referrer data and extracts insertAffiliate parameter
     * @param activity The activity context
     * @param rawReferrer The raw referrer string from Play Store
     */
    private static void processInstallReferrerData(Activity activity, String rawReferrer) {
        verboseLog("Processing install referrer data...");
        
        try {
            // Parse the referrer string directly for insertAffiliate parameter
            String insertAffiliate = null;
            
            // Look for insertAffiliate=value in the raw referrer string
            if (rawReferrer.contains("insertAffiliate=")) {
                String[] params = rawReferrer.split("&");
                for (String param : params) {
                    if (param.startsWith("insertAffiliate=")) {
                        insertAffiliate = param.substring("insertAffiliate=".length());
                        break;
                    }
                }
            }
            
            verboseLog("Extracted insertAffiliate parameter: " + insertAffiliate);
            
            // If we have insertAffiliate parameter, use it as the affiliate identifier
            if (insertAffiliate != null && !insertAffiliate.isEmpty()) {
                verboseLog("Found insertAffiliate parameter, setting as affiliate identifier: " + insertAffiliate);
                setInsertAffiliateIdentifier(activity, insertAffiliate);
            } else {
                verboseLog("No insertAffiliate parameter found in referrer data");
            }
            
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error processing install referrer data: " + e.getMessage());
            verboseLog("Error processing referrer data: " + e.getMessage());
        }
    }

    // MARK: Event Tracking
    public static String trackEvent(Activity activity, String eventName) {
        verboseLog("Tracking event: " + eventName);
        
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Company code is not set. Please initialise the SDK with a valid company code.");
            verboseLog("Cannot track event: no company code available");
            return "[Insert Affiliate] Company code is not set. Please initialise the SDK with a valid company code.";
        }
        
        Log.i("InsertAffiliate TAG", "track event called with - companyCode: " + companyCode);
        
        String deepLinkParam = returnInsertAffiliateIdentifier(activity);
        if (deepLinkParam == null) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events.");
            verboseLog("Cannot track event: no affiliate identifier available");
            return "[Insert Affiliate] No affiliate identifier found. Please set one before tracking events by opening a link from an affiliate.";
        }
        
        verboseLog("Deep link param: " + deepLinkParam);

        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty("eventName", eventName);
        jsonParams.addProperty("companyId", companyCode);

        // URL encode the deepLinkParam if the Android version supports it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            deepLinkParam = URLEncoder.encode(deepLinkParam, StandardCharsets.UTF_8);
        }

        jsonParams.addProperty("deepLinkParam", deepLinkParam);
        
        verboseLog("Track event payload: " + jsonParams.toString());
        verboseLog("Making API call to track event...");

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
                verboseLog("Track event API response status: " + responseCode);
                Log.d("InsertAffiliate response: ", "" + response.body());

                if (responseCode == 200) {
                    responseMessage = "[Insert Affiliate] Track Event Success";
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Event tracked successfully");
                    verboseLog("Event tracked successfully on server");
                } else {
                    responseMessage = "[Insert Affiliate] Failed to track event with status code: " + responseCode;
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Failed to track event with status code: " + responseCode);
                    verboseLog("Track event API error: status " + responseCode + ", response: " + response.body());
                }
            }

            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.i("InsertAffiliate TAG", "Error While Tracking Event");
                verboseLog("Network error tracking event: " + t.getMessage());
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

    // MARK: Offer Codes
    /**
     * Fetches an offer code from the Insert Affiliate API for the given affiliate link
     * @param affiliateLink The affiliate link to fetch the offer code for
     * @param callback Callback that receives the offer code (null if not found or error)
     */
    public static void fetchOfferCode(String affiliateLink, OfferCodeCallback callback) {
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Cannot fetch offer code: no company code available");
            callback.onOfferCodeReceived(null);
            return;
        }
        
        if (affiliateLink == null || affiliateLink.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to encode affiliate link");
            callback.onOfferCodeReceived(null);
            return;
        }

        String encodedAffiliateLink;
        try {
            encodedAffiliateLink = URLEncoder.encode(affiliateLink, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to encode affiliate link");
            callback.onOfferCodeReceived(null);
            return;
        }

        String offerCodeUrlString = "https://api.insertaffiliate.com/v1/affiliateReturnOfferCode/" + companyCode + "/" + encodedAffiliateLink + "?platformType=android";

        try {
            URL offerCodeUrl = new URL(offerCodeUrlString);
            
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) offerCodeUrl.openConnection();
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

                        String rawOfferCode = response.toString();

                        // Check for specific error strings from API
                        if (rawOfferCode.contains("errorofferCodeNotFound") ||
                            rawOfferCode.contains("errorAffiliateoffercodenotfoundinanycompany") ||
                            rawOfferCode.contains("errorAffiliateoffercodenotfoundinanycompanyAffiliatelinkwas") ||
                            rawOfferCode.contains("Routenotfound")) {
                            Log.i("InsertAffiliate TAG", "[Insert Affiliate] Offer code not found or invalid: " + rawOfferCode);
                            callback.onOfferCodeReceived(null);
                        } else {
                            String cleanedOfferCode = cleanOfferCode(rawOfferCode);
                            Log.i("InsertAffiliate TAG", "[Insert Affiliate] Successfully fetched and cleaned offer code: " + cleanedOfferCode);
                            callback.onOfferCodeReceived(cleanedOfferCode);
                        }
                    } else {
                        Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error fetching offer code: HTTP " + responseCode);
                        callback.onOfferCodeReceived(null);
                    }
                } catch (Exception e) {
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error fetching offer code: " + e.getMessage());
                    callback.onOfferCodeReceived(null);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();
        } catch (MalformedURLException e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Invalid offer code URL");
            callback.onOfferCodeReceived(null);
        }
    }

    /**
     * Retrieves and stores an offer code for the given affiliate link
     * @param activity The activity context
     * @param affiliateLink The affiliate link to fetch the offer code for
     */
    public static void retrieveAndStoreOfferCode(Activity activity, String affiliateLink) {
        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Attempting to retrieve and store offer code for: " + affiliateLink);
        
        fetchOfferCode(affiliateLink, new OfferCodeCallback() {
            @Override
            public void onOfferCodeReceived(String offerCode) {
                SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                
                if (offerCode != null && !offerCode.isEmpty()) {
                    // Store the offer code
                    editor.putString("offer_code", offerCode);
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Successfully stored offer code: " + offerCode);
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] Offer code retrieved and stored successfully");
                } else {
                    Log.i("InsertAffiliate TAG", "[Insert Affiliate] No valid offer code found to store");
                    // Clear stored offer code if none found
                    editor.putString("offer_code", "");
                }
                
                editor.apply();
            }
        });
    }
    
    /**
     * Gets the stored offer code from SharedPreferences
     * @param activity The activity context
     * @return The stored offer code, or null if none exists
     */
    public static String getStoredOfferCode(Activity activity) {
        try {
            SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
            String offerCode = sharedPreferences.getString("offer_code", null);
            return (offerCode != null && !offerCode.isEmpty()) ? offerCode : null;
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error getting stored offer code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cleans an offer code by removing special characters, keeping only alphanumeric, underscores, and dashes
     * @param offerCode The offer code to clean
     * @return The cleaned offer code
     */
    private static String cleanOfferCode(String offerCode) {
        if (offerCode == null) {
            return "";
        }
        // Remove special characters, keep only alphanumeric, underscores, and dashes
        return offerCode.replaceAll("[^a-zA-Z0-9_-]", "");
    }
    
    /**
     * Removes special characters from a string, keeping only alphanumeric characters
     * @param input The input string to clean
     * @return The cleaned string with only alphanumeric characters
     * @deprecated Use cleanOfferCode instead
     */
    private static String removeSpecialCharacters(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Helper method for verbose logging
     * @param message The message to log if verbose logging is enabled
     */
    private static void verboseLog(String message) {
        if (verboseLogging) {
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] [VERBOSE] " + message);
        }
    }

    /**
     * Checks if the current affiliate attribution is still valid based on timeout settings
     * @param activity The activity context
     * @return true if attribution is valid, false if expired or no timeout configured
     */
    public static boolean isAffiliateAttributionValid(Activity activity) {
        // If no timeout is configured, attribution is always valid
        if (affiliateAttributionActiveTime <= 0) {
            verboseLog("No timeout configured, attribution is valid");
            return true;
        }
        
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        long storedDate = sharedPreferences.getLong("affiliate_stored_date", 0);
        
        if (storedDate == 0) {
            verboseLog("No stored date found, attribution is invalid");
            return false;
        }
        
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long timeDifference = currentTimeSeconds - storedDate;
        
        boolean isValid = timeDifference <= affiliateAttributionActiveTime;
        verboseLog("Attribution validity check - stored: " + storedDate + ", current: " + currentTimeSeconds + ", difference: " + timeDifference + "s, timeout: " + affiliateAttributionActiveTime + "s, valid: " + isValid);
        
        return isValid;
    }
    
    /**
     * Gets the date when the affiliate identifier was stored
     * @param activity The activity context
     * @return The timestamp in seconds since epoch when affiliate was stored, or 0 if not found
     */
    public static long getAffiliateStoredDate(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("InsertAffiliate", Context.MODE_PRIVATE);
        long storedDate = sharedPreferences.getLong("affiliate_stored_date", 0);
        verboseLog("Getting affiliate stored date: " + storedDate);
        return storedDate;
    }

    /**
     * Safely notifies the affiliate identifier change callback
     * @param activity The activity context to get the current identifier
     */
    private static void notifyIdentifierChange(Activity activity) {
        InsertAffiliateIdentifierChangeCallback callback = identifierChangeCallback;
        if (callback != null) {
            String identifier = returnInsertAffiliateIdentifier(activity);
            callbackExecutor.execute(() -> {
                try {
                    callback.onIdentifierChanged(identifier);
                    verboseLog("Notified callback of identifier change: " + identifier);
                } catch (Exception e) {
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error in identifier change callback: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Callback interface for offer code fetching operations
     */
    public interface OfferCodeCallback {
        void onOfferCodeReceived(String offerCode);
    }

    /**
     * Handles deep links containing insertAffiliate parameter
     * This method should be called from Activity.onCreate() and Activity.onNewIntent()
     * @param activity The activity context
     * @param intent The intent containing the deep link data
     */
    public static void handleInsertLink(Activity activity, Intent intent) {
        if (intent == null || intent.getData() == null) {
            verboseLog("No intent or URI data found in handleInsertLink");
            return;
        }
        
        Uri uri = intent.getData();
        verboseLog("InsertAffiliate: Processing Insert Link URI: " + uri.toString());
        
        // Look for insertAffiliate parameter in the URI
        String insertAffiliate = uri.getQueryParameter("insertAffiliate");
        
        if (insertAffiliate != null && !insertAffiliate.isEmpty()) {
            verboseLog("Found insertAffiliate parameter: " + insertAffiliate);
            Log.i("InsertAffiliate TAG", "[Insert Affiliate] Deep link detected with insertAffiliate parameter: " + insertAffiliate);
            
            // Set the affiliate identifier using the found parameter
            setInsertAffiliateIdentifier(activity, insertAffiliate);
        } else {
            verboseLog("No insertAffiliate parameter found in deep link");
        }
    }

    /**
     * Callback interface for affiliate identifier change notifications
     */
    public interface InsertAffiliateIdentifierChangeCallback {
        void onIdentifierChanged(String identifier);
    }

    /**
     * Callback interface for short code validation
     */
    public interface ShortCodeValidationCallback {
        void onValidationComplete(boolean isValid);
    }

    /**
     * Callback interface for affiliate details fetching operations
     */
    public interface AffiliateDetailsCallback {
        void onAffiliateDetailsReceived(AffiliateDetails details);
    }

    /**
     * Data class for affiliate details
     */
    public static class AffiliateDetails {
        private String affiliateName;
        private String affiliateShortCode;
        private String deeplinkUrl;

        public AffiliateDetails(String affiliateName, String affiliateShortCode, String deeplinkUrl) {
            this.affiliateName = affiliateName;
            this.affiliateShortCode = affiliateShortCode;
            this.deeplinkUrl = deeplinkUrl;
        }

        public String getAffiliateName() {
            return affiliateName;
        }

        public String getAffiliateShortCode() {
            return affiliateShortCode;
        }

        public String getDeeplinkUrl() {
            return deeplinkUrl;
        }
    }

    /**
     * Fetches affiliate details for a given short code without setting it
     * @param shortCode The short code to fetch details for
     * @param callback Callback that receives the affiliate details (null if not found or error)
     */
    public static void getAffiliateDetails(String shortCode, AffiliateDetailsCallback callback) {
        if (companyCode == null || companyCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Cannot get affiliate details: no company code available");
            callback.onAffiliateDetailsReceived(null);
            return;
        }

        if (shortCode == null || shortCode.isEmpty()) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Short code cannot be null or empty");
            callback.onAffiliateDetailsReceived(null);
            return;
        }

        // Convert short code to uppercase
        String capitalisedShortCode = shortCode.toUpperCase();

        // Validate short code format
        if (capitalisedShortCode.length() < 3 || capitalisedShortCode.length() > 25) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Short code must be between 3 and 25 characters long");
            callback.onAffiliateDetailsReceived(null);
            return;
        }

        if (!capitalisedShortCode.matches("^[a-zA-Z0-9]+$")) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Short code must contain only letters and numbers");
            callback.onAffiliateDetailsReceived(null);
            return;
        }

        String apiUrl = "https://api.insertaffiliate.com/V1/checkAffiliateExists";

        // Build JSON payload
        JSONObject payload = new JSONObject();
        try {
            payload.put("companyId", companyCode);
            payload.put("affiliateCode", capitalisedShortCode);
        } catch (Exception e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] Failed to build JSON payload: " + e.getMessage());
            callback.onAffiliateDetailsReceived(null);
            return;
        }

        verboseLog("Getting affiliate details for: " + capitalisedShortCode);

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
                verboseLog("Affiliate details response status: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    verboseLog("Affiliate details response: " + jsonResponse.toString());

                    // Check if affiliate exists
                    boolean exists = jsonResponse.optBoolean("exists", false);
                    if (exists && jsonResponse.has("affiliate")) {
                        JSONObject affiliate = jsonResponse.getJSONObject("affiliate");
                        String affiliateName = affiliate.optString("affiliateName", "");
                        String affiliateShortCode = affiliate.optString("affiliateShortCode", capitalisedShortCode);
                        String deeplinkUrl = affiliate.optString("deeplinkurl", "");

                        AffiliateDetails details = new AffiliateDetails(affiliateName, affiliateShortCode, deeplinkUrl);
                        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Successfully fetched affiliate details for: " + affiliateName);
                        callback.onAffiliateDetailsReceived(details);
                    } else {
                        Log.i("InsertAffiliate TAG", "[Insert Affiliate] Affiliate not found for short code: " + capitalisedShortCode);
                        callback.onAffiliateDetailsReceived(null);
                    }
                } else {
                    Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error fetching affiliate details: HTTP " + responseCode);
                    callback.onAffiliateDetailsReceived(null);
                }
            } catch (Exception e) {
                Log.e("InsertAffiliate TAG", "[Insert Affiliate] Error fetching affiliate details: " + e.getMessage());
                callback.onAffiliateDetailsReceived(null);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}
