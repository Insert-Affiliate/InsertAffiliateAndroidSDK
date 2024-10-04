package com.aks.insertaffiliateandroid;

public class ReceiptVerificationCredentials {
    private final String appName;
    private final String secretKey;

    // Constructor to initialize appName and secretKey
    public ReceiptVerificationCredentials(String appName, String secretKey) {
        this.appName = appName;    // Initialize appName
        this.secretKey = secretKey; // Initialize secretKey
    }

    // Getter for appName
    public String getAppName() {
        return appName;
    }

    // Getter for secretKey
    public String getSecretKey() {
        return secretKey;
    }
}