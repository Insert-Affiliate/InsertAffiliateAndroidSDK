package com.aks.insertaffiliateandroid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The text on the drop-in "Refer a friend" screen, so apps can translate it or
 * reword it. Pass overrides with ReferAFriendOptions.setStrings or setString,
 * keyed by the constants below. Every key is optional: a missing, null or blank
 * override uses the English default, and an unknown key is ignored.
 *
 *   Map&lt;String, String&gt; strings = new HashMap&lt;&gt;();
 *   strings.put(ReferralStrings.SHARE_BUTTON, "Partager");
 *   InsertAffiliateManager.showReferAFriend(this, new ReferAFriendOptions().setStrings(strings));
 *
 * The headline and the reward text are not here: they come from the dashboard
 * and are overridden with setHeadline and setRewardText.
 *
 * Placeholders inside a value are kept as they are and filled in by the screen,
 * so a translation must keep them: {email} in CODE_SENT_NOTICE and {date} in
 * PREMIUM_UNTIL.
 */
public final class ReferralStrings {

    // Joining
    public static final String EMAIL_LABEL = "emailLabel";
    public static final String NAME_LABEL = "nameLabel";
    public static final String JOIN_BUTTON = "joinButton";

    // Email code step
    public static final String CODE_LABEL = "codeLabel";
    public static final String CODE_SENT_NOTICE = "codeSentNotice";
    public static final String VERIFY_BUTTON = "verifyButton";
    public static final String RESEND_BUTTON = "resendButton";
    public static final String CODE_RESENT_NOTICE = "codeResentNotice";
    public static final String DIFFERENT_EMAIL_BUTTON = "differentEmailButton";

    // Joined
    public static final String CODE_LABEL_TITLE = "codeLabelTitle";
    public static final String LINK_LABEL_TITLE = "linkLabelTitle";
    public static final String COPY_BUTTON = "copyButton";
    public static final String COPIED_NOTICE = "copiedNotice";
    public static final String SHARE_BUTTON = "shareButton";
    public static final String REFERRALS_LABEL = "referralsLabel";
    public static final String EARNED_LABEL = "earnedLabel";
    public static final String PREMIUM_UNTIL = "premiumUntil";
    public static final String REWARDS_HEADING = "rewardsHeading";
    public static final String REDEEM_BUTTON = "redeemButton";
    public static final String DASHBOARD_LINK = "dashboardLink";

    // Frame and states
    public static final String CLOSE_BUTTON = "closeButton";
    public static final String TRY_AGAIN_BUTTON = "tryAgainButton";
    public static final String BUSY_BUTTON = "busyButton";

    // Errors, one per server error code
    public static final String ERROR_PROGRAM_DISABLED = "errorProgramDisabled";
    public static final String ERROR_AFFILIATE_LIMIT_REACHED = "errorAffiliateLimitReached";
    public static final String ERROR_INVALID_CODE = "errorInvalidCode";
    public static final String ERROR_TOO_MANY_CODES = "errorTooManyCodes";
    public static final String ERROR_RATE_LIMITED = "errorRateLimited";
    public static final String ERROR_INVALID_EMAIL = "errorInvalidEmail";
    public static final String ERROR_NETWORK = "errorNetwork";
    public static final String ERROR_SERVER = "errorServer";

    private static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> defaults = new HashMap<>();
        defaults.put(EMAIL_LABEL, "Email");
        defaults.put(NAME_LABEL, "Name");
        defaults.put(JOIN_BUTTON, "Get my link");

        defaults.put(CODE_LABEL, "6-digit code");
        defaults.put(CODE_SENT_NOTICE, "We sent a 6-digit code to {email}. Enter it below to connect your account.");
        defaults.put(VERIFY_BUTTON, "Verify");
        defaults.put(RESEND_BUTTON, "Send a new code");
        defaults.put(CODE_RESENT_NOTICE, "New code sent");
        defaults.put(DIFFERENT_EMAIL_BUTTON, "Use a different email");

        defaults.put(CODE_LABEL_TITLE, "Your code");
        defaults.put(LINK_LABEL_TITLE, "Your link");
        defaults.put(COPY_BUTTON, "Copy");
        defaults.put(COPIED_NOTICE, "Copied");
        defaults.put(SHARE_BUTTON, "Share");
        defaults.put(REFERRALS_LABEL, "Referrals");
        defaults.put(EARNED_LABEL, "Earned");
        defaults.put(PREMIUM_UNTIL, "Free premium until {date}");
        defaults.put(REWARDS_HEADING, "Your rewards");
        defaults.put(REDEEM_BUTTON, "Redeem");
        defaults.put(DASHBOARD_LINK, "Open my dashboard");

        defaults.put(CLOSE_BUTTON, "Close");
        defaults.put(TRY_AGAIN_BUTTON, "Try again");
        defaults.put(BUSY_BUTTON, "Please wait...");

        defaults.put(ERROR_PROGRAM_DISABLED, "Referrals are not available in this app right now.");
        defaults.put(ERROR_AFFILIATE_LIMIT_REACHED, "The referral program is full right now. Please try again later.");
        defaults.put(ERROR_INVALID_CODE, "That code is wrong or has expired.");
        defaults.put(ERROR_TOO_MANY_CODES, "Too many codes requested. Please wait a while and try again.");
        defaults.put(ERROR_RATE_LIMITED, "Too many attempts. Please try again later.");
        defaults.put(ERROR_INVALID_EMAIL, "Please enter a valid email address.");
        defaults.put(ERROR_NETWORK, "Could not connect. Check your connection and try again.");
        defaults.put(ERROR_SERVER, "Something went wrong. Please try again.");
        DEFAULTS = Collections.unmodifiableMap(defaults);
    }

    private ReferralStrings() {}

    /** The English text for a key, or "" when the key is not one of the constants above. */
    public static String defaultValue(String key) {
        String value = key == null ? null : DEFAULTS.get(key);
        return value == null ? "" : value;
    }

    /** Every key with its English default. */
    public static Map<String, String> defaults() {
        return DEFAULTS;
    }

    /** The key holding the message for a server error code, for example PROGRAM_DISABLED. */
    static String errorKey(String errorCode) {
        if (errorCode == null) {
            return ERROR_SERVER;
        }
        switch (errorCode) {
            case "PROGRAM_DISABLED":
                return ERROR_PROGRAM_DISABLED;
            case "AFFILIATE_LIMIT_REACHED":
                return ERROR_AFFILIATE_LIMIT_REACHED;
            case "INVALID_CODE":
                return ERROR_INVALID_CODE;
            case "TOO_MANY_CODES":
                return ERROR_TOO_MANY_CODES;
            case "RATE_LIMITED":
                return ERROR_RATE_LIMITED;
            case "INVALID_EMAIL":
                return ERROR_INVALID_EMAIL;
            case AffiliateUserResult.ERROR_NETWORK:
                return ERROR_NETWORK;
            default:
                return ERROR_SERVER;
        }
    }

    // The app's override for a key, or the English default when it is missing or blank.
    static String resolve(Map<String, String> strings, String key) {
        if (strings != null) {
            String override = strings.get(key);
            if (override != null && !override.trim().isEmpty()) {
                return override.trim();
            }
        }
        return defaultValue(key);
    }
}
