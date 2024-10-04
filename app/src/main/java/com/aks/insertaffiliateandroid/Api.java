package com.aks.insertaffiliateandroid;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface Api {

    String BASE_URL = "https://validator.iaptic.com/";

    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @POST("v1/validate")
    Call<JsonObject> validaterec(@Body JsonObject rawJsonString,
                                 @Header("Authorization") String yourIapticAuthHeader);
}