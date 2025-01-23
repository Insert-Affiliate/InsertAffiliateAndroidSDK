package com.aks.insertaffiliateandroid;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface Api {
        String BASE_URL_IAPTIC_VALIDATOR = "https://validator.iaptic.com/";
        String BASE_URL_INSERT_AFFILIATE = "https://api.insertaffiliate.com/";

        @Headers({
                "Accept: application/json",
                "Content-Type: application/json"
        })
        @POST("v1/validate")
        Call<JsonObject> validaterec(@Body JsonObject rawJsonString,
                                        @Header("Authorization") String yourIapticAuthHeader);

        @Headers({ 
                "Accept: application/json",
                "Content-Type: application/json"
        })
        @POST("v1/trackEvent")
        Call<JsonObject> trackevent(@Body JsonObject rawJsonString);
}