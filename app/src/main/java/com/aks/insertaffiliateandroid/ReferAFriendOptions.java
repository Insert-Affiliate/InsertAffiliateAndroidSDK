package com.aks.insertaffiliateandroid;

import android.graphics.Color;
import android.graphics.Typeface;

/**
 * Options for showReferAFriend. Every option is optional; setters return this
 * so they can be chained:
 *
 *   new ReferAFriendOptions().setEmail(user.email).setName(user.name)
 *
 * Headline, reward text and colour fall back to the values set in the
 * Insert Affiliate dashboard, then to the defaults.
 */
public class ReferAFriendOptions {
    private String email;
    private String name;
    private String shareMessage;
    private Integer primaryColor;
    private String headline;
    private String rewardText;
    private Typeface typeface;
    private float cornerRadiusDp = 16f;
    private Runnable onClose;
    private String appUserId;
    private String playPurchaseToken;

    /** Prefills the email field, usually with your logged-in user's email. */
    public ReferAFriendOptions setEmail(String email) {
        this.email = email;
        return this;
    }

    /** Prefills the name field. */
    public ReferAFriendOptions setName(String name) {
        this.name = name;
        return this;
    }

    /** Text shared with the link. May use {link} and {code} placeholders. */
    public ReferAFriendOptions setShareMessage(String shareMessage) {
        this.shareMessage = shareMessage;
        return this;
    }

    /** Overrides the dashboard colour. */
    public ReferAFriendOptions setPrimaryColor(int color) {
        this.primaryColor = color;
        return this;
    }

    /** Overrides the dashboard colour with a hex string such as "#6A0DAD". Ignored if invalid. */
    public ReferAFriendOptions setPrimaryColor(String hexColor) {
        try {
            this.primaryColor = Color.parseColor(hexColor);
        } catch (Exception e) {
            this.primaryColor = null;
        }
        return this;
    }

    /** Overrides the dashboard headline. */
    public ReferAFriendOptions setHeadline(String headline) {
        this.headline = headline;
        return this;
    }

    /** Overrides the dashboard reward text. */
    public ReferAFriendOptions setRewardText(String rewardText) {
        this.rewardText = rewardText;
        return this;
    }

    /** Font for all text on the screen. */
    public ReferAFriendOptions setTypeface(Typeface typeface) {
        this.typeface = typeface;
        return this;
    }

    /** Corner radius of the sheet, buttons and fields, in dp. Default 16. */
    public ReferAFriendOptions setCornerRadius(float cornerRadiusDp) {
        this.cornerRadiusDp = cornerRadiusDp;
        return this;
    }

    /** Called on the main thread when the screen closes. */
    public ReferAFriendOptions setOnClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    /**
     * Your user's RevenueCat app user id or Adapty customer user id, sent when
     * they join, and saved with setReferrerAccount when the screen opens for a
     * user who already joined, so waiting referrer rewards are granted.
     */
    public ReferAFriendOptions setAppUserId(String appUserId) {
        this.appUserId = appUserId;
        return this;
    }

    /** Your user's own Google Play subscription purchase token. Sent like setAppUserId. */
    public ReferAFriendOptions setPlayPurchaseToken(String playPurchaseToken) {
        this.playPurchaseToken = playPurchaseToken;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getShareMessage() {
        return shareMessage;
    }

    public Integer getPrimaryColor() {
        return primaryColor;
    }

    public String getHeadline() {
        return headline;
    }

    public String getRewardText() {
        return rewardText;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public float getCornerRadius() {
        return cornerRadiusDp;
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public String getPlayPurchaseToken() {
        return playPurchaseToken;
    }

    // The referrer's accounts for createAffiliateForUser, verifyAffiliateCode
    // and setReferrerAccount, or null when neither is set.
    ReferrerAccountOptions referrerAccount() {
        boolean hasUser = appUserId != null && !appUserId.trim().isEmpty();
        boolean hasToken = playPurchaseToken != null && !playPurchaseToken.trim().isEmpty();
        if (!hasUser && !hasToken) {
            return null;
        }
        return new ReferrerAccountOptions().setAppUserId(appUserId).setPlayPurchaseToken(playPurchaseToken);
    }
}
