package com.aks.insertaffiliateandroid;

/**
 * Result of createAffiliateForUser and verifyAffiliateCode.
 *
 * status is one of:
 *   created               the user is now an affiliate and this device is connected
 *   connected             an existing affiliate reconnected with the emailed code
 *   verificationRequired  a 6-digit code was emailed; call verifyAffiliateCode next
 *   error                 see getErrorCode() and getErrorMessage()
 */
public class AffiliateUserResult {
    public static final String STATUS_CREATED = "created";
    public static final String STATUS_CONNECTED = "connected";
    public static final String STATUS_VERIFICATION_REQUIRED = "verificationRequired";
    public static final String STATUS_ERROR = "error";

    // Error codes raised by the SDK itself. Server codes (PROGRAM_DISABLED,
    // AFFILIATE_LIMIT_REACHED, INVALID_CODE, TOO_MANY_CODES, ...) pass through as sent.
    public static final String ERROR_NETWORK = "NETWORK_ERROR";
    public static final String ERROR_SERVER = "SERVER_ERROR";
    public static final String ERROR_NOT_INITIALIZED = "NOT_INITIALIZED";

    private final String status;
    private final InsertAffiliateManager.AffiliateDetails affiliate;
    private final String errorCode;
    private final String errorMessage;

    public AffiliateUserResult(String status, InsertAffiliateManager.AffiliateDetails affiliate, String errorCode, String errorMessage) {
        this.status = status;
        this.affiliate = affiliate;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    static AffiliateUserResult error(String errorCode, String errorMessage) {
        return new AffiliateUserResult(STATUS_ERROR, null, errorCode, errorMessage);
    }

    public String getStatus() {
        return status;
    }

    /** The affiliate's name, short code and link. Null unless created or connected. */
    public InsertAffiliateManager.AffiliateDetails getAffiliate() {
        return affiliate;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** True when the device is now connected (created or connected). */
    public boolean isSuccess() {
        return STATUS_CREATED.equals(status) || STATUS_CONNECTED.equals(status);
    }

    public boolean isVerificationRequired() {
        return STATUS_VERIFICATION_REQUIRED.equals(status);
    }
}
