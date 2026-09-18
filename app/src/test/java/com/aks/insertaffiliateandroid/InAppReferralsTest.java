package com.aks.insertaffiliateandroid;

import org.junit.Test;

import static org.junit.Assert.*;

public class InAppReferralsTest {

    // MARK: enrol / verify

    @Test
    public void enrolCreated_returnsAffiliateAndToken() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(200,
            "{\"status\":\"created\",\"token\":\"tok123\",\"affiliate\":{\"affiliateName\":\"Jane\",\"affiliateShortCode\":\"A1B2C3D4\",\"deeplinkurl\":\"https://insertaffiliate.link/x\"}}");

        assertEquals("tok123", parsed.token);
        assertTrue(parsed.result.isSuccess());
        assertEquals(AffiliateUserResult.STATUS_CREATED, parsed.result.getStatus());
        assertEquals("Jane", parsed.result.getAffiliate().getAffiliateName());
        assertEquals("A1B2C3D4", parsed.result.getAffiliate().getAffiliateShortCode());
        assertEquals("https://insertaffiliate.link/x", parsed.result.getAffiliate().getDeeplinkUrl());
        assertNull(parsed.result.getErrorCode());
    }

    @Test
    public void verifyConnected_isSuccess() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(200,
            "{\"status\":\"connected\",\"token\":\"t\",\"affiliate\":{\"affiliateName\":\"Jane\",\"affiliateShortCode\":\"CODE1\",\"deeplinkurl\":\"\"}}");

        assertEquals(AffiliateUserResult.STATUS_CONNECTED, parsed.result.getStatus());
        assertTrue(parsed.result.isSuccess());
        assertEquals("t", parsed.token);
    }

    @Test
    public void verificationRequired_hasNoTokenOrAffiliate() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(200, "{\"status\":\"verificationRequired\"}");

        assertNull(parsed.token);
        assertTrue(parsed.result.isVerificationRequired());
        assertFalse(parsed.result.isSuccess());
        assertNull(parsed.result.getAffiliate());
    }

    @Test
    public void serverError_passesCodeAndMessageThrough() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(403,
            "{\"error\":\"In-app referrals are not enabled for this app.\",\"code\":\"PROGRAM_DISABLED\"}");

        assertNull(parsed.token);
        assertEquals(AffiliateUserResult.STATUS_ERROR, parsed.result.getStatus());
        assertEquals("PROGRAM_DISABLED", parsed.result.getErrorCode());
        assertEquals("In-app referrals are not enabled for this app.", parsed.result.getErrorMessage());
    }

    @Test
    public void errorWithoutCode_isServerError() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(500, "{\"error\":\"Failed to set up referrals.\"}");
        assertEquals(AffiliateUserResult.ERROR_SERVER, parsed.result.getErrorCode());

        InAppReferrals.EnrolResponse html = InAppReferrals.parseEnrolResponse(502, "<html>Bad gateway</html>");
        assertEquals(AffiliateUserResult.ERROR_SERVER, html.result.getErrorCode());
    }

    @Test
    public void noResponse_isNetworkError() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(-1, null);
        assertEquals(AffiliateUserResult.ERROR_NETWORK, parsed.result.getErrorCode());
        assertNull(parsed.token);
    }

    @Test
    public void createdWithoutToken_isNotTreatedAsSuccess() {
        InAppReferrals.EnrolResponse parsed = InAppReferrals.parseEnrolResponse(200, "{\"status\":\"created\"}");
        assertFalse(parsed.result.isSuccess());
        assertNull(parsed.token);
    }

    // MARK: me / config

    @Test
    public void parsesMyAffiliateDetails() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails(
            "{\"affiliateName\":\"Jane\",\"affiliateShortCode\":\"a1b2c3d4\",\"deeplinkurl\":\"https://insertaffiliate.link/abc\"," +
            "\"referralTrigger\":\"purchase\",\"referralCount\":7,\"installCount\":19,\"purchaseCount\":7,\"eventCount\":11," +
            "\"totalEarned\":42.5,\"totalPaid\":30,\"totalUnpaid\":12.5,\"currency\":\"USD\"," +
            "\"dashboardUrl\":\"https://app.insertaffiliate.com/signin\"}");

        assertNotNull(details);
        assertEquals("Jane", details.getAffiliateName());
        assertEquals("a1b2c3d4", details.getAffiliateShortCode());
        assertEquals("https://insertaffiliate.link/abc", details.getDeeplinkUrl());
        assertEquals("purchase", details.getReferralTrigger());
        assertEquals(7, details.getReferralCount());
        assertEquals(19, details.getInstallCount());
        assertEquals(7, details.getPurchaseCount());
        assertEquals(11, details.getEventCount());
        assertEquals(42.5, details.getTotalEarned(), 0.0001);
        assertEquals(30.0, details.getTotalPaid(), 0.0001);
        assertEquals(12.5, details.getTotalUnpaid(), 0.0001);
        assertEquals("USD", details.getCurrency());
        assertEquals("https://app.insertaffiliate.com/signin", details.getDashboardUrl());
    }

    @Test
    public void myAffiliateDetails_toleratesMissingAndNullFields() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails(
            "{\"affiliateName\":null,\"referralCount\":\"x\",\"currency\":\"EUR\"}");

        assertNotNull(details);
        assertEquals("", details.getAffiliateName());
        assertEquals("", details.getDeeplinkUrl());
        assertEquals(0, details.getReferralCount());
        assertEquals(0.0, details.getTotalEarned(), 0.0001);
        assertEquals("EUR", details.getCurrency());
    }

    @Test
    public void myAffiliateDetails_invalidJsonIsNull() {
        assertNull(InAppReferrals.parseMyAffiliateDetails("not json"));
        assertNull(InAppReferrals.parseMyAffiliateDetails(""));
        assertNull(InAppReferrals.parseMyAffiliateDetails("[1,2]"));
    }

    @Test
    public void parsesConfig() {
        ReferralProgramConfig config = InAppReferrals.parseConfig(
            "{\"enabled\":true,\"companyName\":\"Velvet\",\"referralTrigger\":\"event\",\"headline\":\"Invite friends\"," +
            "\"rewardText\":\"Get a free month\",\"primaryColor\":\"#FF0000\"}");

        assertTrue(config.isEnabled());
        assertEquals("Velvet", config.getCompanyName());
        assertEquals("event", config.getReferralTrigger());
        assertEquals("Invite friends", config.getHeadline());
        assertEquals("Get a free month", config.getRewardText());
        assertEquals("#FF0000", config.getPrimaryColor());
    }

    @Test
    public void config_enabledOnlyWhenTrueBoolean() {
        assertFalse(InAppReferrals.parseConfig("{\"enabled\":false}").isEnabled());
        assertFalse(InAppReferrals.parseConfig("{\"enabled\":\"true\"}").isEnabled());
        assertFalse(InAppReferrals.parseConfig("{}").isEnabled());
    }

    @Test
    public void readsErrorCode() {
        assertEquals("INVALID_TOKEN", InAppReferrals.errorCode("{\"error\":\"Not connected.\",\"code\":\"INVALID_TOKEN\"}"));
        assertEquals("", InAppReferrals.errorCode(null));
    }

    // MARK: share text

    @Test
    public void shareText_webLinkDefault() {
        assertEquals("Try Velvet: https://insertaffiliate.link/abc",
            InAppReferrals.buildShareText("https://insertaffiliate.link/abc", "A1B2", "Velvet", null));
    }

    @Test
    public void shareText_webLinkCustomMessageAppendsLink() {
        assertEquals("Get a free week! https://insertaffiliate.link/abc",
            InAppReferrals.buildShareText("https://insertaffiliate.link/abc", "A1B2", "Velvet", "Get a free week!"));
    }

    @Test
    public void shareText_customMessagePlaceholders() {
        assertEquals("Use A1B2 or tap https://insertaffiliate.link/abc",
            InAppReferrals.buildShareText("https://insertaffiliate.link/abc", "A1B2", "Velvet", "Use {code} or tap {link}"));
    }

    @Test
    public void shareText_shortCodeOnly() {
        // Short Code Only companies store the short code as the deeplinkurl.
        assertEquals("Use my code A1B2 in Velvet",
            InAppReferrals.buildShareText("A1B2", "A1B2", "Velvet", null));
    }

    @Test
    public void shareText_noLinkYet() {
        assertEquals("Use my code A1B2 in Velvet",
            InAppReferrals.buildShareText("", "A1B2", "Velvet", null));
    }

    @Test
    public void shareText_shortCodeOnlyCustomMessage() {
        assertEquals("Join me with code A1B2",
            InAppReferrals.buildShareText("A1B2", "A1B2", "Velvet", "Join me with code {code}"));
        assertEquals("Join me! A1B2",
            InAppReferrals.buildShareText("A1B2", "A1B2", "Velvet", "Join me!"));
    }

    @Test
    public void shareText_missingCompanyName() {
        assertEquals("Try this app: https://x.link/a",
            InAppReferrals.buildShareText("https://x.link/a", "A1B2", "", null));
        assertEquals("Use my code A1B2",
            InAppReferrals.buildShareText("", "A1B2", null, null));
    }

    @Test
    public void shareText_blankMessageUsesDefault() {
        assertEquals("Try Velvet: https://x.link/a",
            InAppReferrals.buildShareText("https://x.link/a", "A1B2", "Velvet", "   "));
    }

    // MARK: copy

    @Test
    public void errorMessages_haveNoEmDashes() {
        String[] codes = {"PROGRAM_DISABLED", "AFFILIATE_LIMIT_REACHED", "INVALID_CODE", "TOO_MANY_CODES",
            "RATE_LIMITED", "INVALID_EMAIL", "NETWORK_ERROR", "SERVER_ERROR", null};
        for (String code : codes) {
            String message = InAppReferrals.messageForError(code);
            assertFalse(message.isEmpty());
            assertFalse(message.contains("—"));
        }
        assertEquals("That code is wrong or has expired.", InAppReferrals.messageForError("INVALID_CODE"));
    }
}
