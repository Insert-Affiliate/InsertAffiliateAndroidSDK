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

    public ReferralProgramConfig(boolean enabled, String companyName, String referralTrigger, String headline, String rewardText, String primaryColor) {
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
}
