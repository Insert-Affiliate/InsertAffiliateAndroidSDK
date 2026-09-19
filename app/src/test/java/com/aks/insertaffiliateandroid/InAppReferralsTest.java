package com.aks.insertaffiliateandroid;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.List;

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

    // MARK: Phase 2: rewards on /me

    @Test
    public void myAffiliateDetails_parsesRewards() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails(
            "{\"affiliateName\":\"Jane\",\"rewardsGranted\":3,\"premiumUntil\":\"2026-10-19T12:00:00.000Z\"," +
            "\"rewardCodes\":[{\"code\":\"NEWER\",\"redeemUrl\":\"https://apps.apple.com/redeem?ctx=offercodes&id=1&code=NEWER\",\"grantedAt\":\"2026-09-18T10:00:00.000Z\"}," +
            "{\"code\":\"OLDER\",\"redeemUrl\":\"https://apps.apple.com/redeem?code=OLDER\",\"grantedAt\":\"2026-08-01T10:00:00.000Z\"}]}");

        assertEquals(3, details.getRewardsGranted());
        assertEquals("2026-10-19T12:00:00.000Z", details.getPremiumUntil());
        assertEquals(1792411200000L, details.getPremiumUntilDate().getTime());

        List<MyAffiliateDetails.RewardCode> codes = details.getRewardCodes();
        assertEquals(2, codes.size());
        assertEquals("NEWER", codes.get(0).getCode());
        assertEquals("https://apps.apple.com/redeem?ctx=offercodes&id=1&code=NEWER", codes.get(0).getRedeemUrl());
        assertEquals("2026-09-18T10:00:00.000Z", codes.get(0).getGrantedAt());
        assertEquals("OLDER", codes.get(1).getCode());
    }

    @Test
    public void myAffiliateDetails_rewardsDefaultWhenMissing() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails("{\"affiliateName\":\"Jane\"}");

        assertEquals(0, details.getRewardsGranted());
        assertNull(details.getPremiumUntil());
        assertNull(details.getPremiumUntilDate());
        assertNotNull(details.getRewardCodes());
        assertTrue(details.getRewardCodes().isEmpty());
    }

    @Test
    public void myAffiliateDetails_rewardsAreLenient() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails(
            "{\"rewardsGranted\":\"two\",\"premiumUntil\":null," +
            "\"rewardCodes\":[null,5,{\"redeemUrl\":\"https://x\"},{\"code\":\"ONLY\",\"redeemUrl\":null}]}");

        assertEquals(0, details.getRewardsGranted());
        assertNull(details.getPremiumUntil());
        assertEquals(1, details.getRewardCodes().size());
        assertEquals("ONLY", details.getRewardCodes().get(0).getCode());
        assertEquals("", details.getRewardCodes().get(0).getRedeemUrl());
        assertEquals("", details.getRewardCodes().get(0).getGrantedAt());

        MyAffiliateDetails notArray = InAppReferrals.parseMyAffiliateDetails("{\"rewardCodes\":{\"code\":\"X\"},\"premiumUntil\":\"\"}");
        assertTrue(notArray.getRewardCodes().isEmpty());
        assertNull(notArray.getPremiumUntil());
    }

    @Test
    public void rewardCodes_parseStore() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails(
            "{\"rewardCodes\":[" +
            "{\"code\":\"PLAY1\",\"redeemUrl\":\"https://play.google.com/redeem?code=PLAY1\",\"store\":\"google_play\",\"grantedAt\":\"2026-09-19T10:00:00.000Z\"}," +
            "{\"code\":\"APPLE1\",\"redeemUrl\":\"https://apps.apple.com/redeem?code=APPLE1\",\"store\":\"app_store\"}," +
            "{\"code\":\"LEGACY\",\"redeemUrl\":\"https://apps.apple.com/redeem?code=LEGACY\"}," +
            "{\"code\":\"BLANK\",\"store\":\"\"}," +
            "{\"code\":\"NULLSTORE\",\"store\":null}," +
            "{\"code\":\"FUTURE\",\"store\":\"amazon_appstore\"}]}");

        List<MyAffiliateDetails.RewardCode> codes = details.getRewardCodes();
        assertEquals(6, codes.size());

        assertEquals(MyAffiliateDetails.RewardCode.STORE_GOOGLE_PLAY, codes.get(0).getStore());
        assertTrue(codes.get(0).isGooglePlay());
        assertEquals("https://play.google.com/redeem?code=PLAY1", codes.get(0).getRedeemUrl());

        assertEquals(MyAffiliateDetails.RewardCode.STORE_APP_STORE, codes.get(1).getStore());
        assertFalse(codes.get(1).isGooglePlay());

        // Missing, blank or null store means an App Store code.
        assertEquals(MyAffiliateDetails.RewardCode.STORE_APP_STORE, codes.get(2).getStore());
        assertEquals(MyAffiliateDetails.RewardCode.STORE_APP_STORE, codes.get(3).getStore());
        assertEquals(MyAffiliateDetails.RewardCode.STORE_APP_STORE, codes.get(4).getStore());

        // Unknown stores pass through and are not treated as Google Play.
        assertEquals("amazon_appstore", codes.get(5).getStore());
        assertFalse(codes.get(5).isGooglePlay());
    }

    @Test
    public void rewardCode_threeArgConstructorDefaultsToAppStore() {
        MyAffiliateDetails.RewardCode code = new MyAffiliateDetails.RewardCode("X", "https://x", "");
        assertEquals(MyAffiliateDetails.RewardCode.STORE_APP_STORE, code.getStore());
        assertFalse(code.isGooglePlay());
    }

    @Test
    public void googlePlayRewardCodes_keepsOnlyPlayCodesInOrder() {
        MyAffiliateDetails details = InAppReferrals.parseMyAffiliateDetails(
            "{\"rewardCodes\":[" +
            "{\"code\":\"PLAY_NEW\",\"store\":\"google_play\"}," +
            "{\"code\":\"APPLE\",\"store\":\"app_store\"}," +
            "{\"code\":\"LEGACY\"}," +
            "{\"code\":\"FUTURE\",\"store\":\"amazon_appstore\"}," +
            "{\"code\":\"PLAY_OLD\",\"store\":\"google_play\"}]}");

        List<MyAffiliateDetails.RewardCode> play = InAppReferrals.googlePlayRewardCodes(details.getRewardCodes());
        assertEquals(2, play.size());
        assertEquals("PLAY_NEW", play.get(0).getCode());
        assertEquals("PLAY_OLD", play.get(1).getCode());
    }

    @Test
    public void googlePlayRewardCodes_emptyWhenNoneOrNull() {
        MyAffiliateDetails appleOnly = InAppReferrals.parseMyAffiliateDetails(
            "{\"rewardCodes\":[{\"code\":\"APPLE\"}]}");
        assertTrue(InAppReferrals.googlePlayRewardCodes(appleOnly.getRewardCodes()).isEmpty());
        assertTrue(InAppReferrals.googlePlayRewardCodes(null).isEmpty());
    }

    @Test
    public void premiumUntil_acceptsMillisAndFirestoreTimestamps() {
        MyAffiliateDetails millis = InAppReferrals.parseMyAffiliateDetails("{\"premiumUntil\":1792411200000}");
        assertEquals(1792411200000L, millis.getPremiumUntilDate().getTime());

        MyAffiliateDetails firestore = InAppReferrals.parseMyAffiliateDetails("{\"premiumUntil\":{\"_seconds\":1792411200,\"_nanoseconds\":0}}");
        assertEquals(1792411200000L, firestore.getPremiumUntilDate().getTime());
    }

    @Test
    public void parseTimeMillis_readsIsoVariants() {
        assertEquals(Long.valueOf(1792411200000L), InAppReferrals.parseTimeMillis("2026-10-19T12:00:00Z"));
        assertEquals(Long.valueOf(1792411200000L), InAppReferrals.parseTimeMillis("2026-10-19T13:00:00+01:00"));
        assertEquals(Long.valueOf(1792411200000L), InAppReferrals.parseTimeMillis("2026-10-19T12:00:00"));
        assertEquals(Long.valueOf(1792368000000L), InAppReferrals.parseTimeMillis("2026-10-19"));
        assertNull(InAppReferrals.parseTimeMillis("soon"));
        assertNull(InAppReferrals.parseTimeMillis(""));
        assertNull(InAppReferrals.parseTimeMillis(null));
    }

    @Test
    public void isPremiumActive_onlyForFutureDates() {
        long now = 1792411200000L;
        assertTrue(InAppReferrals.isPremiumActive("2026-10-20T00:00:00Z", now));
        assertFalse(InAppReferrals.isPremiumActive("2026-10-19T12:00:00Z", now));
        assertFalse(InAppReferrals.isPremiumActive("2026-01-01T00:00:00Z", now));
        assertFalse(InAppReferrals.isPremiumActive(null, now));
        assertFalse(InAppReferrals.isPremiumActive("not a date", now));
    }

    // MARK: Phase 2: request bodies

    @Test
    public void identityBody_hasAccountsAndDeviceId() {
        JsonObject body = InAppReferrals.identityBody(
            new ReferrerAccountOptions().setAppUserId(" rc_user_1 ").setPlayPurchaseToken("abcdefghij.klmnop"), "a1b2c3");

        assertEquals("rc_user_1", body.get("appUserId").getAsString());
        assertEquals("abcdefghij.klmnop", body.get("playPurchaseToken").getAsString());
        assertEquals("a1b2c3", body.get("deviceId").getAsString());
        assertEquals(3, body.size());
    }

    @Test
    public void identityBody_leavesOutEmptyValues() {
        JsonObject onlyDevice = InAppReferrals.identityBody(null, "a1b2c3");
        assertEquals("{\"deviceId\":\"a1b2c3\"}", onlyDevice.toString());

        JsonObject blanks = InAppReferrals.identityBody(
            new ReferrerAccountOptions().setAppUserId("  ").setPlayPurchaseToken(""), null);
        assertEquals(0, blanks.size());

        JsonObject onlyUser = InAppReferrals.identityBody(new ReferrerAccountOptions().setAppUserId("u1"), null);
        assertEquals("{\"appUserId\":\"u1\"}", onlyUser.toString());
    }

    @Test
    public void addReferrerAccount_keepsEnrolFields() {
        JsonObject body = new JsonObject();
        body.addProperty("companyId", "company1");
        body.addProperty("email", "jane@example.com");
        InAppReferrals.addReferrerAccount(body, new ReferrerAccountOptions().setAppUserId("u1"), "a1b2c3");

        assertEquals("company1", body.get("companyId").getAsString());
        assertEquals("jane@example.com", body.get("email").getAsString());
        assertEquals("u1", body.get("appUserId").getAsString());
        assertEquals("a1b2c3", body.get("deviceId").getAsString());
        assertFalse(body.has("playPurchaseToken"));
    }

    @Test
    public void parseIdentitySaved_onlyTrueForSavedTrue() {
        assertTrue(InAppReferrals.parseIdentitySaved(200, "{\"saved\":true}"));
        assertFalse(InAppReferrals.parseIdentitySaved(200, "{\"saved\":false}"));
        assertFalse(InAppReferrals.parseIdentitySaved(200, "{\"saved\":\"true\"}"));
        assertFalse(InAppReferrals.parseIdentitySaved(200, "not json"));
        assertFalse(InAppReferrals.parseIdentitySaved(500, "{\"saved\":true}"));
        assertFalse(InAppReferrals.parseIdentitySaved(-1, null));
    }
}
