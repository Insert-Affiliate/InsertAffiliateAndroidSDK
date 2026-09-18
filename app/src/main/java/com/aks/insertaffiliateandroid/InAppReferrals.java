package com.aks.insertaffiliateandroid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
            string(json, "dashboardUrl")
        );
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
        if (errorCode == null) {
            return "Something went wrong. Please try again.";
        }
        switch (errorCode) {
            case "PROGRAM_DISABLED":
                return "Referrals are not available in this app right now.";
            case "AFFILIATE_LIMIT_REACHED":
                return "The referral program is full right now. Please try again later.";
            case "INVALID_CODE":
                return "That code is wrong or has expired.";
            case "TOO_MANY_CODES":
                return "Too many codes requested. Please wait a while and try again.";
            case "RATE_LIMITED":
                return "Too many attempts. Please try again later.";
            case "INVALID_EMAIL":
                return "Please enter a valid email address.";
            case AffiliateUserResult.ERROR_NETWORK:
                return "Could not connect. Check your connection and try again.";
            default:
                return "Something went wrong. Please try again.";
        }
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
