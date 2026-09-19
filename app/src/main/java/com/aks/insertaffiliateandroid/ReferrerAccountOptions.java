package com.aks.insertaffiliateandroid;

/**
 * The referrer's own accounts, passed to createAffiliateForUser,
 * verifyAffiliateCode and setReferrerAccount. Every option is optional;
 * setters return this so they can be chained:
 *
 *   new ReferrerAccountOptions().setAppUserId(Purchases.getSharedInstance().getAppUserID())
 *
 * The server uses these to grant the referrer's rewards and to spot a
 * "friend" who is really the referrer.
 */
public class ReferrerAccountOptions {
    private String appUserId;
    private String playPurchaseToken;

    /** Your user's RevenueCat app user id or Adapty customer user id. */
    public ReferrerAccountOptions setAppUserId(String appUserId) {
        this.appUserId = appUserId;
        return this;
    }

    /** Your user's own Google Play subscription purchase token. */
    public ReferrerAccountOptions setPlayPurchaseToken(String playPurchaseToken) {
        this.playPurchaseToken = playPurchaseToken;
        return this;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public String getPlayPurchaseToken() {
        return playPurchaseToken;
    }
}
