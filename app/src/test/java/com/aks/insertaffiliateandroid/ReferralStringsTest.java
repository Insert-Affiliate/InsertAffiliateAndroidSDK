package com.aks.insertaffiliateandroid;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ReferralStringsTest {

    @Test
    public void noOverrides_useTheEnglishDefaults() {
        ReferAFriendOptions options = new ReferAFriendOptions();
        assertEquals("Email", options.string(ReferralStrings.EMAIL_LABEL));
        assertEquals("Get my link", options.string(ReferralStrings.JOIN_BUTTON));
        assertEquals("Your code", options.string(ReferralStrings.CODE_LABEL_TITLE));
        assertEquals("Your link", options.string(ReferralStrings.LINK_LABEL_TITLE));
        assertEquals("Share", options.string(ReferralStrings.SHARE_BUTTON));
        assertEquals("Close", options.string(ReferralStrings.CLOSE_BUTTON));
        assertEquals("Please wait...", options.string(ReferralStrings.BUSY_BUTTON));
        assertTrue(options.getStrings().isEmpty());
    }

    @Test
    public void oneOverride_leavesTheRestAsDefaults() {
        ReferAFriendOptions options = new ReferAFriendOptions().setString(ReferralStrings.SHARE_BUTTON, "Partager");
        assertEquals("Partager", options.string(ReferralStrings.SHARE_BUTTON));
        assertEquals("Copy", options.string(ReferralStrings.COPY_BUTTON));
        assertEquals("Verify", options.string(ReferralStrings.VERIFY_BUTTON));
    }

    @Test
    public void setStrings_addsToWhatIsAlreadySet() {
        Map<String, String> first = new HashMap<>();
        first.put(ReferralStrings.EMAIL_LABEL, "Courriel");
        Map<String, String> second = new HashMap<>();
        second.put(ReferralStrings.NAME_LABEL, "Nom");

        ReferAFriendOptions options = new ReferAFriendOptions().setStrings(first).setStrings(second);

        assertEquals("Courriel", options.string(ReferralStrings.EMAIL_LABEL));
        assertEquals("Nom", options.string(ReferralStrings.NAME_LABEL));
        assertEquals("Get my link", options.string(ReferralStrings.JOIN_BUTTON));
    }

    @Test
    public void blankOrNullOverride_fallsBackToTheDefault() {
        ReferAFriendOptions options = new ReferAFriendOptions()
            .setString(ReferralStrings.COPY_BUTTON, "   ")
            .setString(ReferralStrings.REDEEM_BUTTON, "")
            .setString(ReferralStrings.EARNED_LABEL, null);

        assertEquals("Copy", options.string(ReferralStrings.COPY_BUTTON));
        assertEquals("Redeem", options.string(ReferralStrings.REDEEM_BUTTON));
        assertEquals("Earned", options.string(ReferralStrings.EARNED_LABEL));
    }

    @Test
    public void overrideIsTrimmed() {
        ReferAFriendOptions options = new ReferAFriendOptions().setString(ReferralStrings.SHARE_BUTTON, "  Enviar  ");
        assertEquals("Enviar", options.string(ReferralStrings.SHARE_BUTTON));
    }

    @Test
    public void unknownKeys_areIgnored() {
        Map<String, String> strings = new HashMap<>();
        strings.put("notAKey", "whatever");
        ReferAFriendOptions options = new ReferAFriendOptions().setStrings(strings).setStrings(null).setString(null, "x");

        assertEquals("Email", options.string(ReferralStrings.EMAIL_LABEL));
        assertEquals("", ReferralStrings.defaultValue("notAKey"));
        assertEquals("", ReferralStrings.defaultValue(null));
    }

    @Test
    public void defaultsKeepTheirPlaceholders() {
        assertTrue(ReferralStrings.defaultValue(ReferralStrings.CODE_SENT_NOTICE).contains("{email}"));
        assertTrue(ReferralStrings.defaultValue(ReferralStrings.PREMIUM_UNTIL).contains("{date}"));
        // An override keeps its placeholder for the screen to fill in.
        ReferAFriendOptions options = new ReferAFriendOptions()
            .setString(ReferralStrings.PREMIUM_UNTIL, "Premium gratuit jusqu'au {date}");
        assertEquals("Premium gratuit jusqu'au 19 Oct 2026",
            options.string(ReferralStrings.PREMIUM_UNTIL).replace("{date}", "19 Oct 2026"));
        assertEquals("We sent a 6-digit code to jane@example.com. Enter it below to connect your account.",
            options.string(ReferralStrings.CODE_SENT_NOTICE).replace("{email}", "jane@example.com"));
    }

    @Test
    public void errorStrings_keyOffTheServerCode() {
        ReferAFriendOptions options = new ReferAFriendOptions()
            .setString(ReferralStrings.ERROR_INVALID_CODE, "Code invalide.");

        assertEquals("Code invalide.", options.errorString("INVALID_CODE"));
        assertEquals("Referrals are not available in this app right now.", options.errorString("PROGRAM_DISABLED"));
        assertEquals("The referral program is full right now. Please try again later.",
            options.errorString("AFFILIATE_LIMIT_REACHED"));
        assertEquals("Too many codes requested. Please wait a while and try again.", options.errorString("TOO_MANY_CODES"));
        assertEquals("Too many attempts. Please try again later.", options.errorString("RATE_LIMITED"));
        assertEquals("Please enter a valid email address.", options.errorString("INVALID_EMAIL"));
        assertEquals("Could not connect. Check your connection and try again.",
            options.errorString(AffiliateUserResult.ERROR_NETWORK));
        // Anything else, including an unknown code, shows the server error text.
        assertEquals("Something went wrong. Please try again.", options.errorString("DEEP_LINK_POOL_CONFLICT"));
        assertEquals("Something went wrong. Please try again.", options.errorString(null));
    }

    @Test
    public void defaults_matchTheTextTheScreenShowedBefore() {
        // messageForError still answers with the defaults, so apps that read it are unaffected.
        assertEquals(InAppReferrals.messageForError("INVALID_CODE"),
            ReferralStrings.defaultValue(ReferralStrings.ERROR_INVALID_CODE));
        assertEquals(ReferralStrings.defaults().size(), ReferralStrings.defaults().keySet().size());
        for (Map.Entry<String, String> entry : ReferralStrings.defaults().entrySet()) {
            assertFalse(entry.getValue().isEmpty());
            assertFalse(entry.getValue().contains("—"));
        }
    }
}
