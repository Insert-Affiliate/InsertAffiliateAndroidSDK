package com.aks.insertaffiliateandroid;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The signed-in user's own affiliate details and referral stats, returned by
 * getMyAffiliateDetails.
 *
 * These values are for display. Grant anything valuable from your server using
 * the referral.created webhook or the Public API, because a modified device can
 * fake what it shows.
 */
public class MyAffiliateDetails {
    private final String affiliateName;
    private final String affiliateShortCode;
    private final String deeplinkUrl;
    private final String referralTrigger;
    private final int referralCount;
    private final int installCount;
    private final int eventCount;
    private final int purchaseCount;
    private final double totalEarned;
    private final double totalPaid;
    private final double totalUnpaid;
    private final String currency;
    private final String dashboardUrl;
    private final int rewardsGranted;
    private final String premiumUntil;
    private final List<RewardCode> rewardCodes;

    /** An App Store one-time offer code granted to the referrer as a reward. */
    public static final class RewardCode {
        private final String code;
        private final String redeemUrl;
        private final String grantedAt;

        public RewardCode(String code, String redeemUrl, String grantedAt) {
            this.code = code;
            this.redeemUrl = redeemUrl;
            this.grantedAt = grantedAt;
        }

        public String getCode() {
            return code;
        }

        /** Where the code is redeemed. May be empty. */
        public String getRedeemUrl() {
            return redeemUrl;
        }

        /** When the code was granted, as an ISO 8601 string. May be empty. */
        public String getGrantedAt() {
            return grantedAt;
        }
    }

    public MyAffiliateDetails(
        String affiliateName,
        String affiliateShortCode,
        String deeplinkUrl,
        String referralTrigger,
        int referralCount,
        int installCount,
        int eventCount,
        int purchaseCount,
        double totalEarned,
        double totalPaid,
        double totalUnpaid,
        String currency,
        String dashboardUrl
    ) {
        this(affiliateName, affiliateShortCode, deeplinkUrl, referralTrigger, referralCount, installCount,
            eventCount, purchaseCount, totalEarned, totalPaid, totalUnpaid, currency, dashboardUrl, 0, null, null);
    }

    public MyAffiliateDetails(
        String affiliateName,
        String affiliateShortCode,
        String deeplinkUrl,
        String referralTrigger,
        int referralCount,
        int installCount,
        int eventCount,
        int purchaseCount,
        double totalEarned,
        double totalPaid,
        double totalUnpaid,
        String currency,
        String dashboardUrl,
        int rewardsGranted,
        String premiumUntil,
        List<RewardCode> rewardCodes
    ) {
        this.affiliateName = affiliateName;
        this.affiliateShortCode = affiliateShortCode;
        this.deeplinkUrl = deeplinkUrl;
        this.referralTrigger = referralTrigger;
        this.referralCount = referralCount;
        this.installCount = installCount;
        this.eventCount = eventCount;
        this.purchaseCount = purchaseCount;
        this.totalEarned = totalEarned;
        this.totalPaid = totalPaid;
        this.totalUnpaid = totalUnpaid;
        this.currency = currency;
        this.dashboardUrl = dashboardUrl;
        this.rewardsGranted = rewardsGranted;
        this.premiumUntil = premiumUntil == null || premiumUntil.isEmpty() ? null : premiumUntil;
        this.rewardCodes = rewardCodes == null ? Collections.emptyList() : Collections.unmodifiableList(rewardCodes);
    }

    public String getAffiliateName() {
        return affiliateName;
    }

    public String getAffiliateShortCode() {
        return affiliateShortCode;
    }

    /** The referral link, or the short code itself for Short Code Only apps. May be empty. */
    public String getDeeplinkUrl() {
        return deeplinkUrl;
    }

    /** What counts as a referral for this app: install, event or purchase. */
    public String getReferralTrigger() {
        return referralTrigger;
    }

    /** Referrals for the app's configured trigger. Only ever goes up. */
    public int getReferralCount() {
        return referralCount;
    }

    public int getInstallCount() {
        return installCount;
    }

    public int getEventCount() {
        return eventCount;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public double getTotalUnpaid() {
        return totalUnpaid;
    }

    public String getCurrency() {
        return currency;
    }

    /** Where the user signs in to their affiliate dashboard. */
    public String getDashboardUrl() {
        return dashboardUrl;
    }

    /** How many referrer rewards the user has been granted. */
    public int getRewardsGranted() {
        return rewardsGranted;
    }

    /** When the user's free premium from referrer rewards ends, as an ISO 8601 string, or null. */
    public String getPremiumUntil() {
        return premiumUntil;
    }

    /** getPremiumUntil as a Date, or null when absent or unreadable. */
    public Date getPremiumUntilDate() {
        Long millis = InAppReferrals.parseTimeMillis(premiumUntil);
        return millis == null ? null : new Date(millis);
    }

    /**
     * App Store one-time offer codes granted as rewards, newest first. Never null.
     * They can only be redeemed on iOS.
     */
    public List<RewardCode> getRewardCodes() {
        return rewardCodes;
    }
}
