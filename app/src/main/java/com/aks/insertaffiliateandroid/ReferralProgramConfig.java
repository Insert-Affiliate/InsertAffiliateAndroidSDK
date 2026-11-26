package com.aks.insertaffiliateandroid;

/**
 * The app's in-app referral settings from the Insert Affiliate dashboard,
 * returned by getReferralProgramConfig. Public values only.
 */
public class ReferralProgramConfig {
    private final boolean enabled;
    private final String companyName;
    private final String referralTrigger;
    private final String headline;
    private final String rewardText;
    private final String primaryColor;
    private final boolean showCode;

    public ReferralProgramConfig(boolean enabled, String companyName, String referralTrigger, String headline, String rewardText, String primaryColor) {
        this(enabled, companyName, referralTrigger, headline, rewardText, primaryColor, true);
    }

    public ReferralProgramConfig(boolean enabled, String companyName, String referralTrigger, String headline, String rewardText, String primaryColor, boolean showCode) {
        this.showCode = showCode;
        this.enabled = enabled;
        this.companyName = companyName;
        this.referralTrigger = referralTrigger;
        this.headline = headline;
        this.rewardText = rewardText;
        this.primaryColor = primaryColor;
    }

    /** False when the company has not switched in-app referrals on. */
    public boolean isEnabled() {
        return enabled;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getReferralTrigger() {
        return referralTrigger;
    }

    public String getHeadline() {
        return headline;
    }

    public String getRewardText() {
        return rewardText;
    }

    /** Hex colour such as "#6A0DAD", or empty when not set. */
    public String getPrimaryColor() {
        return primaryColor;
    }

    /** False when the dashboard hides the short code on the screen. True when an older server does not say. */
    public boolean isShowCode() {
        return showCode;
    }
}
