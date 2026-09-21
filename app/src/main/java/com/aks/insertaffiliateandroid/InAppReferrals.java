package com.aks.insertaffiliateandroid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-app referrals: response parsing, share text and error copy. Kept free of
 * Android APIs so it runs in plain JVM unit tests.
 */
final class InAppReferrals {
    static final String DEFAULT_HEADLINE = "Refer a friend";
    static final String DEFAULT_PRIMARY_COLOR = "#6A0DAD";
    static final String PLATFORM = "android";

    private InAppReferrals() {}

    /** A parsed enrol/verify response. The token stays inside the SDK. */
    static final class EnrolResponse {
        final AffiliateUserResult result;
        final String token;

        EnrolResponse(AffiliateUserResult result, String token) {
            this.result = result;
            this.token = token;
        }
    }

    // MARK: Parsing

    static JsonObject parseObject(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses POST /enrol and POST /verify. httpStatus is the HTTP status code,
     * or -1 when the request never reached the server.
     */
    static EnrolResponse parseEnrolResponse(int httpStatus, String body) {
        if (httpStatus < 0) {
            return new EnrolResponse(
                AffiliateUserResult.error(AffiliateUserResult.ERROR_NETWORK, "Could not reach Insert Affiliate."), null);
        }
        JsonObject json = parseObject(body);

        if (httpStatus < 200 || httpStatus >= 300) {
            return new EnrolResponse(errorFrom(json), null);
        }
        if (json == null) {
            return new EnrolResponse(
                AffiliateUserResult.error(AffiliateUserResult.ERROR_SERVER, "Unexpected response from Insert Affiliate."), null);
        }

        String status = string(json, "status");
        if (AffiliateUserResult.STATUS_VERIFICATION_REQUIRED.equals(status)) {
            return new EnrolResponse(
                new AffiliateUserResult(AffiliateUserResult.STATUS_VERIFICATION_REQUIRED, null, null, null), null);
        }

        String token = string(json, "token");
        if ((AffiliateUserResult.STATUS_CREATED.equals(status) || AffiliateUserResult.STATUS_CONNECTED.equals(status))
                && !token.isEmpty()) {
            JsonObject affiliate = object(json, "affiliate");
            return new EnrolResponse(
                new AffiliateUserResult(status, affiliateFrom(affiliate), null, null), token);
        }
        return new EnrolResponse(
            AffiliateUserResult.error(AffiliateUserResult.ERROR_SERVER, "Unexpected response from Insert Affiliate."), null);
    }

    /** Parses GET /me. Returns null when the body is not a JSON object. */
    static MyAffiliateDetails parseMyAffiliateDetails(String body) {
        JsonObject json = parseObject(body);
        if (json == null) {
            return null;
        }
        return new MyAffiliateDetails(
            string(json, "affiliateName"),
            string(json, "affiliateShortCode"),
            string(json, "deeplinkurl"),
            string(json, "referralTrigger"),
            integer(json, "referralCount"),
            integer(json, "installCount"),
            integer(json, "eventCount"),
            integer(json, "purchaseCount"),
            number(json, "totalEarned"),
            number(json, "totalPaid"),
            number(json, "totalUnpaid"),
            string(json, "currency"),
            string(json, "dashboardUrl"),
            integer(json, "rewardsGranted"),
            timestamp(json, "premiumUntil"),
            rewardCodes(json)
        );
    }

    /** Parses POST /me/identity: true only for a 2xx response with saved: true. */
    static boolean parseIdentitySaved(int httpStatus, String body) {
        if (httpStatus < 200 || httpStatus >= 300) {
            return false;
        }
        JsonObject json = parseObject(body);
        JsonElement saved = json == null ? null : json.get("saved");
        return saved != null && saved.isJsonPrimitive() && saved.getAsJsonPrimitive().isBoolean() && saved.getAsBoolean();
    }

    // MARK: Email code

    static final int EMAIL_CODE_LENGTH = 6;

    /**
     * The emailed code as ASCII digits: every Unicode decimal digit (for example
     * Eastern Arabic digits) becomes 0-9 and everything else (spaces, dashes) is
     * dropped, so "123-456" and "\u0661\u0662\u0663 \u0664\u0665\u0666" both give "123456".
     */
    static String normalizeEmailCode(String code) {
        if (code == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < code.length(); ) {
            int c = code.codePointAt(i);
            int digit = Character.digit(c, 10);
            if (digit >= 0 && Character.isDigit(c)) {
                digits.append((char) ('0' + digit));
            }
            i += Character.charCount(c);
        }
        return digits.toString();
    }

    /** True when the code has exactly six digits once normalised. */
    static boolean isCompleteEmailCode(String code) {
        return normalizeEmailCode(code).length() == EMAIL_CODE_LENGTH;
    }

    // MARK: Request bodies

    /**
     * Adds the referrer's accounts to an enrol, verify or identity body.
     * deviceId is the device id from the "{shortCode}-{deviceId}" insert
     * affiliate identifier. Empty values are left out.
     */
    static void addReferrerAccount(JsonObject body, ReferrerAccountOptions options, String deviceId) {
        putIfPresent(body, "deviceId", deviceId);
        if (options != null) {
            putIfPresent(body, "appUserId", options.getAppUserId());
            putIfPresent(body, "playPurchaseToken", options.getPlayPurchaseToken());
        }
    }

    /** Body for POST /me/identity: { appUserId?, playPurchaseToken?, deviceId }. */
    static JsonObject identityBody(ReferrerAccountOptions options, String deviceId) {
        JsonObject body = new JsonObject();
        addReferrerAccount(body, options, deviceId);
        return body;
    }

    private static void putIfPresent(JsonObject body, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            body.addProperty(key, value.trim());
        }
    }

    // MARK: Dates

    /**
     * Epoch millis for an ISO 8601 date or date-time (with Z, an offset, or
     * neither, read as UTC). Null when absent or unreadable.
     */
    static Long parseTimeMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(text).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        return null;
    }

    /** True when premiumUntil is a readable date after nowMillis. */
    static boolean isPremiumActive(String premiumUntil, long nowMillis) {
        Long until = parseTimeMillis(premiumUntil);
        return until != null && until > nowMillis;
    }

    /** Parses GET /config/{companyId}. Returns null when the body is not a JSON object. */
    static ReferralProgramConfig parseConfig(String body) {
        JsonObject json = parseObject(body);
        if (json == null) {
            return null;
        }
        JsonElement enabled = json.get("enabled");
        return new ReferralProgramConfig(
            enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean() && enabled.getAsBoolean(),
            string(json, "companyName"),
            string(json, "referralTrigger"),
            string(json, "headline"),
            string(json, "rewardText"),
            string(json, "primaryColor")
        );
    }

    /** Server error code from an error body, e.g. INVALID_TOKEN. Empty when absent. */
    static String errorCode(String body) {
        return string(parseObject(body), "code");
    }

    /**
     * True when the server says the device token no longer works: HTTP 401
     * with INVALID_TOKEN, or HTTP 404 with AFFILIATE_NOT_FOUND. Any other
     * 401/404 (a proxy, or an API without the route) keeps the token.
     */
    static boolean isConnectionGone(int httpStatus, String body) {
        String code = errorCode(body);
        return (httpStatus == 401 && "INVALID_TOKEN".equals(code))
            || (httpStatus == 404 && "AFFILIATE_NOT_FOUND".equals(code));
    }

    private static AffiliateUserResult errorFrom(JsonObject json) {
        String code = string(json, "code");
        String message = string(json, "error");
        return AffiliateUserResult.error(
            code.isEmpty() ? AffiliateUserResult.ERROR_SERVER : code,
            message.isEmpty() ? "Request failed." : message);
    }

    private static InsertAffiliateManager.AffiliateDetails affiliateFrom(JsonObject affiliate) {
        return new InsertAffiliateManager.AffiliateDetails(
            string(affiliate, "affiliateName"),
            string(affiliate, "affiliateShortCode"),
            string(affiliate, "deeplinkurl"));
    }

    private static JsonObject object(JsonObject json, String key) {
        if (json == null) return null;
        JsonElement value = json.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String string(JsonObject json, String key) {
        if (json == null) return "";
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) return "";
        return value.getAsString();
    }

    private static double number(JsonObject json, String key) {
        if (json == null) return 0;
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return 0;
        return value.getAsDouble();
    }

    private static int integer(JsonObject json, String key) {
        return (int) Math.round(number(json, key));
    }

    // A date as an ISO string, or null. Accepts an ISO string, epoch millis,
    // or a serialised Firestore timestamp ({ _seconds } or { seconds }).
    private static String timestamp(JsonObject json, String key) {
        if (json == null) return null;
        JsonElement value = json.get(key);
        if (value == null || value.isJsonNull()) return null;
        try {
            if (value.isJsonPrimitive()) {
                if (value.getAsJsonPrimitive().isNumber()) {
                    return Instant.ofEpochMilli(value.getAsLong()).toString();
                }
                String text = value.getAsString().trim();
                return text.isEmpty() ? null : text;
            }
            if (value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                double seconds = object.has("_seconds") ? number(object, "_seconds") : number(object, "seconds");
                return seconds > 0 ? Instant.ofEpochMilli((long) (seconds * 1000)).toString() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // rewardCodes: [{ code, redeemUrl, store, grantedAt }]. Entries without a code are skipped.
    // A missing store means an App Store code.
    private static List<MyAffiliateDetails.RewardCode> rewardCodes(JsonObject json) {
        List<MyAffiliateDetails.RewardCode> codes = new ArrayList<>();
        JsonElement value = json == null ? null : json.get("rewardCodes");
        if (value == null || !value.isJsonArray()) return codes;
        JsonArray array = value.getAsJsonArray();
        for (JsonElement item : array) {
            if (item == null || !item.isJsonObject()) continue;
            JsonObject entry = item.getAsJsonObject();
            String code = string(entry, "code");
            if (code.isEmpty()) continue;
            String grantedAt = timestamp(entry, "grantedAt");
            codes.add(new MyAffiliateDetails.RewardCode(code, string(entry, "redeemUrl"), grantedAt == null ? "" : grantedAt, string(entry, "store")));
        }
        return codes;
    }

    /** The reward codes that can be redeemed on Android (Google Play promo codes), in their original order. */
    static List<MyAffiliateDetails.RewardCode> googlePlayRewardCodes(List<MyAffiliateDetails.RewardCode> codes) {
        List<MyAffiliateDetails.RewardCode> playCodes = new ArrayList<>();
        if (codes == null) return playCodes;
        for (MyAffiliateDetails.RewardCode code : codes) {
            if (code != null && code.isGooglePlay()) playCodes.add(code);
        }
        return playCodes;
    }

    // MARK: Share text

    static boolean isWebLink(String deeplinkUrl) {
        return deeplinkUrl != null && deeplinkUrl.toLowerCase(Locale.ROOT).startsWith("http");
    }

    /**
     * The text for the share sheet.
     *
     * With a web link: "<message> <link>", default message "Try {companyName}:".
     * Without one (Short Code Only, or no link assigned yet):
     * "Use my code {code} in {companyName}".
     * An app-supplied message may use {link} and {code}; when it uses neither,
     * the link (or code) is added after it.
     */
    static String buildShareText(String deeplinkUrl, String shortCode, String companyName, String message) {
        String code = shortCode == null ? "" : shortCode;
        String name = companyName == null ? "" : companyName.trim();
        boolean hasLink = isWebLink(deeplinkUrl);
        String link = hasLink ? deeplinkUrl : code;

        if (message != null && !message.trim().isEmpty()) {
            String text = message.trim();
            boolean hasPlaceholder = text.contains("{link}") || text.contains("{code}");
            text = text.replace("{link}", link).replace("{code}", code).replace("{companyName}", name);
            return hasPlaceholder || link.isEmpty() ? text : text + " " + link;
        }

        if (hasLink) {
            String prefix = name.isEmpty() ? "Try this app:" : "Try " + name + ":";
            return prefix + " " + deeplinkUrl;
        }
        return name.isEmpty() ? "Use my code " + code : "Use my code " + code + " in " + name;
    }

    // MARK: Copy

    /** User-facing message for an error code from createAffiliateForUser or verifyAffiliateCode. */
    static String messageForError(String errorCode) {
        return ReferralStrings.defaultValue(ReferralStrings.errorKey(errorCode));
    }

    /** First non-empty value, or "" when all are empty. */
    static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
