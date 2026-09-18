package com.aks.insertaffiliateandroid;

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
}
