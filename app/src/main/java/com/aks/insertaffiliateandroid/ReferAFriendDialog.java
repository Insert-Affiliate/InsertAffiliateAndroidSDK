package com.aks.insertaffiliateandroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * The drop-in "Refer a friend" screen, built in code with standard views so it
 * adds no dependencies and works with any app theme.
 *
 * States: loading, join (email + name, "Get my link"), code (6-digit code,
 * "Verify", "Send a new code"), enrolled (code, link, Copy, Share, stats,
 * "Free premium until" while a referrer reward is active, Google Play
 * reward codes with "Redeem", "Open my dashboard"). Nothing is gated behind sharing and only the system
 * share sheet is used.
 */
final class ReferAFriendDialog {
    private final Activity activity;
    private final ReferAFriendOptions options;
    private final Dialog dialog;
    private final LinearLayout content;

    private final boolean dark;
    private final int backgroundColor;
    private final int textColor;
    private final int secondaryTextColor;
    private final int fieldColor;
    private final int errorColor;

    private ReferralProgramConfig config;
    private int primaryColor;
    private String email;
    private String name;
    private boolean closed;

    ReferAFriendDialog(Activity activity, ReferAFriendOptions options) {
        this.activity = activity;
        this.options = options;
        this.email = options.getEmail() == null ? "" : options.getEmail();
        this.name = options.getName() == null ? "" : options.getName();

        int nightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        dark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        backgroundColor = dark ? Color.parseColor("#1C1C1E") : Color.WHITE;
        textColor = dark ? Color.WHITE : Color.parseColor("#111111");
        secondaryTextColor = dark ? Color.parseColor("#A1A1A6") : Color.parseColor("#5F6368");
        fieldColor = dark ? Color.parseColor("#2C2C2E") : Color.parseColor("#F2F2F5");
        errorColor = dark ? Color.parseColor("#FF6B6B") : Color.parseColor("#C62828");
        primaryColor = resolvePrimaryColor(null);

        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(28));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(backgroundColor);
        float radius = dp(options.getCornerRadius());
        sheet.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        scroll.setBackground(sheet);
        scroll.addView(content, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(scroll);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(d -> {
            closed = true;
            Runnable onClose = options.getOnClose();
            if (onClose != null) onClose.run();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    void show() {
        showLoading();
        dialog.show();
        load();
    }

    // Loads the portal config, then the user's details if this device is connected.
    private void load() {
        InsertAffiliateManager.getReferralProgramConfig(loaded -> runOnUi(() -> {
            config = loaded;
            primaryColor = resolvePrimaryColor(loaded);
            if (!InsertAffiliateManager.isUserAnAffiliate()) {
                showJoin(null);
                return;
            }
            showLoading();
            saveReferrerAccount();
            loadMyDetails(null);
        }));
    }

    // Already joined: saves the app's referrer accounts once, so rewards that
    // were waiting for them (for example Google Play deferrals) are granted.
    private void saveReferrerAccount() {
        ReferrerAccountOptions account = options.referrerAccount();
        if (account != null) {
            InsertAffiliateManager.setReferrerAccount(account, null);
        }
    }

    // fallback: what to show if /me fails right after joining (no stats).
    private void loadMyDetails(InsertAffiliateManager.AffiliateDetails fallback) {
        InsertAffiliateManager.loadMyAffiliateDetails((details, errorCode) -> runOnUi(() -> {
            if (details != null) {
                showEnrolled(details.getAffiliateName(), details.getAffiliateShortCode(), details.getDeeplinkUrl(), details);
            } else if (fallback != null) {
                showEnrolled(fallback.getAffiliateName(), fallback.getAffiliateShortCode(), fallback.getDeeplinkUrl(), null);
            } else if (InsertAffiliateManager.NOT_ENROLLED.equals(errorCode)) {
                showJoin(null);
            } else {
                showLoadError(errorCode);
            }
        }));
    }

    // MARK: States

    private void showLoading() {
        content.removeAllViews();
        addHeader();
        ProgressBar progress = new ProgressBar(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(32);
        params.bottomMargin = dp(32);
        content.addView(progress, params);
    }

    private void showLoadError(String errorCode) {
        content.removeAllViews();
        addHeader();
        addMessage(InAppReferrals.messageForError(errorCode), errorColor);
        Button retry = addPrimaryButton("Try again");
        retry.setOnClickListener(v -> {
            showLoading();
            load();
        });
    }

    private void showJoin(String error) {
        content.removeAllViews();
        addHeader();
        addRewardText();

        if (config != null && !config.isEnabled()) {
            addMessage(InAppReferrals.messageForError("PROGRAM_DISABLED"), secondaryTextColor);
            return;
        }

        EditText emailField = addField("Email", email,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText nameField = addField("Name", name,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        TextView errorView = addError(error);

        Button join = addPrimaryButton("Get my link");
        join.setOnClickListener(v -> {
            email = emailField.getText().toString().trim();
            name = nameField.getText().toString().trim();
            if (email.isEmpty() || !email.contains("@")) {
                setError(errorView, InAppReferrals.messageForError("INVALID_EMAIL"));
                return;
            }
            setBusy(join, true, "Get my link");
            InsertAffiliateManager.createAffiliateForUser(email, name, options.referrerAccount(), result -> runOnUi(() -> handleResult(result, join, "Get my link", errorView)));
        });
    }

    private void showCodeStep(String error) {
        content.removeAllViews();
        addHeader();
        addMessage("We sent a 6-digit code to " + email + ". Enter it below to connect your account.", secondaryTextColor);

        EditText codeField = addField("6-digit code", "", InputType.TYPE_CLASS_NUMBER);
        codeField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        codeField.setGravity(Gravity.CENTER);
        codeField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        codeField.setLetterSpacing(0.3f);
        TextView errorView = addError(error);

        Button verify = addPrimaryButton("Verify");
        verify.setOnClickListener(v -> {
            String code = codeField.getText().toString().trim();
            if (code.length() != 6) {
                setError(errorView, InAppReferrals.messageForError("INVALID_CODE"));
                return;
            }
            setBusy(verify, true, "Verify");
            InsertAffiliateManager.verifyAffiliateCode(email, code, name, options.referrerAccount(), result -> runOnUi(() -> handleResult(result, verify, "Verify", errorView)));
        });

        Button resend = addTextButton("Send a new code");
        resend.setOnClickListener(v -> {
            setBusy(resend, true, "Send a new code");
            InsertAffiliateManager.createAffiliateForUser(email, name, options.referrerAccount(), result -> runOnUi(() -> {
                setBusy(resend, false, "Send a new code");
                if (result.isVerificationRequired()) {
                    setError(errorView, null);
                    toast("New code sent");
                } else {
                    handleResult(result, resend, "Send a new code", errorView);
                }
            }));
        });

        Button changeEmail = addTextButton("Use a different email");
        changeEmail.setOnClickListener(v -> showJoin(null));
    }

    private void handleResult(AffiliateUserResult result, Button button, String label, TextView errorView) {
        if (result.isSuccess()) {
            showLoading();
            loadMyDetails(result.getAffiliate());
        } else if (result.isVerificationRequired()) {
            showCodeStep(null);
        } else {
            setBusy(button, false, label);
            setError(errorView, InAppReferrals.messageForError(result.getErrorCode()));
        }
    }

    private void showEnrolled(String affiliateName, String shortCode, String deeplinkUrl, MyAffiliateDetails details) {
        content.removeAllViews();
        addHeader();
        addRewardText();

        String companyName = config != null ? config.getCompanyName() : "";
        String shareText = InAppReferrals.buildShareText(deeplinkUrl, shortCode, companyName, options.getShareMessage());

        if (shortCode != null && !shortCode.isEmpty()) {
            addLabel("Your code");
            addCopyRow(shortCode, true);
        }
        if (InAppReferrals.isWebLink(deeplinkUrl)) {
            addLabel("Your link");
            addCopyRow(deeplinkUrl, false);
        }

        Button share = addPrimaryButton("Share");
        share.setOnClickListener(v -> InsertAffiliateManager.openShareSheet(activity, shareText));

        if (details != null) {
            addStats(details);
            addPremiumUntil(details);
            addRewardCodes(details);
            String dashboardUrl = details.getDashboardUrl();
            if (dashboardUrl != null && !dashboardUrl.isEmpty()) {
                Button dashboard = addTextButton("Open my dashboard");
                dashboard.setOnClickListener(v -> openUrl(dashboardUrl));
            }
        }
    }

    // MARK: Building blocks

    private void addHeader() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(headline(), 22, textColor, true);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = text("Close", 15, secondaryTextColor, false);
        close.setPadding(dp(12), dp(8), 0, dp(8));
        close.setContentDescription("Close");
        close.setOnClickListener(v -> dialog.dismiss());
        row.addView(close);

        content.addView(row);
    }

    private void addRewardText() {
        String reward = InAppReferrals.firstNonEmpty(options.getRewardText(), config != null ? config.getRewardText() : null);
        if (!reward.isEmpty()) {
            addMessage(reward, secondaryTextColor);
        }
    }

    private void addMessage(String message, int color) {
        TextView view = text(message, 15, color, false);
        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(8);
        content.addView(view, params);
    }

    private void addLabel(String label) {
        TextView view = text(label, 13, secondaryTextColor, false);
        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(18);
        content.addView(view, params);
    }

    private void addCopyRow(String value, boolean large) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(6), dp(6), dp(6));
        row.setBackground(rounded(fieldColor));

        TextView valueView = text(value, large ? 20 : 14, textColor, large);
        valueView.setSingleLine(!large);
        valueView.setTextIsSelectable(true);
        row.addView(valueView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button copy = addTextButtonTo(row, "Copy");
        copy.setOnClickListener(v -> copyToClipboard(value));

        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(6);
        content.addView(row, params);
    }

    private void addStats(MyAffiliateDetails details) {
        LinearLayout strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(dp(8), dp(14), dp(8), dp(14));
        strip.setBackground(rounded(fieldColor));

        strip.addView(stat(String.valueOf(details.getReferralCount()), "Referrals"),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strip.addView(stat(formatMoney(details.getTotalEarned(), details.getCurrency()), "Earned"),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(20);
        content.addView(strip, params);
    }

    private void addPremiumUntil(MyAffiliateDetails details) {
        if (!InAppReferrals.isPremiumActive(details.getPremiumUntil(), System.currentTimeMillis())) {
            return;
        }
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(details.getPremiumUntilDate());
        addMessage("Free premium until " + date, textColor);
    }

    // Only Google Play promo codes: App Store offer codes can't be redeemed on Android.
    private void addRewardCodes(MyAffiliateDetails details) {
        List<MyAffiliateDetails.RewardCode> codes = InAppReferrals.googlePlayRewardCodes(details.getRewardCodes());
        if (codes.isEmpty()) return;
        addLabel("Your rewards");
        for (MyAffiliateDetails.RewardCode reward : codes) {
            addRewardRow(reward);
        }
    }

    private void addRewardRow(MyAffiliateDetails.RewardCode reward) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(6), dp(6), dp(6));
        row.setBackground(rounded(fieldColor));

        TextView codeView = text(reward.getCode(), 17, textColor, true);
        codeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        codeView.setSingleLine(true);
        codeView.setTextIsSelectable(true);
        row.addView(codeView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button copy = addTextButtonTo(row, "Copy");
        copy.setOnClickListener(v -> copyToClipboard(reward.getCode()));

        String redeemUrl = reward.getRedeemUrl();
        if (redeemUrl != null && !redeemUrl.isEmpty()) {
            Button redeem = addTextButtonTo(row, "Redeem");
            redeem.setOnClickListener(v -> openUrl(redeemUrl));
        }

        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(6);
        content.addView(row, params);
    }

    private View stat(String value, String label) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView valueView = text(value, 20, textColor, true);
        valueView.setGravity(Gravity.CENTER);
        TextView labelView = text(label, 13, secondaryTextColor, false);
        labelView.setGravity(Gravity.CENTER);
        column.addView(valueView);
        column.addView(labelView);
        return column;
    }

    private EditText addField(String hint, String value, int inputType) {
        EditText field = new EditText(activity);
        field.setHint(hint);
        field.setText(value);
        field.setInputType(inputType);
        field.setSingleLine(true);
        field.setTextColor(textColor);
        field.setHintTextColor(secondaryTextColor);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.setBackground(rounded(fieldColor));
        if (options.getTypeface() != null) field.setTypeface(options.getTypeface());

        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(12);
        content.addView(field, params);
        return field;
    }

    private TextView addError(String error) {
        TextView view = text("", 14, errorColor, false);
        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(8);
        content.addView(view, params);
        setError(view, error);
        return view;
    }

    private void setError(TextView view, String error) {
        view.setText(error == null ? "" : error);
        view.setVisibility(error == null || error.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private Button addPrimaryButton(String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTextColor(isLight(primaryColor) ? Color.BLACK : Color.WHITE);
        button.setBackground(rounded(primaryColor));
        button.setMinHeight(dp(48));
        button.setStateListAnimator(null);
        Typeface typeface = options.getTypeface();
        button.setTypeface(typeface != null ? typeface : Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(20);
        content.addView(button, params);
        return button;
    }

    private Button addTextButton(String label) {
        Button button = textButton(label);
        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(8);
        content.addView(button, params);
        return button;
    }

    private Button addTextButtonTo(LinearLayout row, String label) {
        Button button = textButton(label);
        row.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return button;
    }

    private Button textButton(String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setTextColor(dark && !isLight(primaryColor) ? Color.WHITE : primaryColor);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setStateListAnimator(null);
        if (options.getTypeface() != null) button.setTypeface(options.getTypeface());
        return button;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        Typeface typeface = options.getTypeface();
        if (typeface != null) {
            view.setTypeface(typeface, bold ? Typeface.BOLD : Typeface.NORMAL);
        } else if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private void setBusy(Button button, boolean busy, String label) {
        button.setEnabled(!busy);
        button.setAlpha(busy ? 0.6f : 1f);
        button.setText(busy ? "Please wait..." : label);
    }

    // MARK: Actions

    private void copyToClipboard(String value) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("Referral", value));
        // Android 13+ shows its own confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            toast("Copied");
        }
    }

    private void openUrl(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Log.e("InsertAffiliate TAG", "[Insert Affiliate] No app can open the link");
        }
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    // MARK: Helpers

    private void runOnUi(Runnable action) {
        activity.runOnUiThread(() -> {
            if (closed || activity.isFinishing() || !dialog.isShowing()) return;
            action.run();
        });
    }

    private String headline() {
        String headline = InAppReferrals.firstNonEmpty(options.getHeadline(), config != null ? config.getHeadline() : null);
        return headline.isEmpty() ? InAppReferrals.DEFAULT_HEADLINE : headline;
    }

    // Options, then the portal config, then the default purple.
    private int resolvePrimaryColor(ReferralProgramConfig loaded) {
        if (options.getPrimaryColor() != null) {
            return options.getPrimaryColor();
        }
        if (loaded != null && !loaded.getPrimaryColor().isEmpty()) {
            try {
                return Color.parseColor(loaded.getPrimaryColor());
            } catch (Exception ignored) {
                // Fall through to the default.
            }
        }
        return Color.parseColor(InAppReferrals.DEFAULT_PRIMARY_COLOR);
    }

    private static boolean isLight(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.7;
    }

    private static String formatMoney(double amount, String currencyCode) {
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance();
            format.setCurrency(Currency.getInstance(currencyCode));
            return format.format(amount);
        } catch (Exception e) {
            String formatted = String.format(Locale.getDefault(), "%.2f", amount);
            return currencyCode == null || currencyCode.isEmpty() ? formatted : formatted + " " + currencyCode;
        }
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(Math.min(options.getCornerRadius(), 24f)));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics()));
    }
}
