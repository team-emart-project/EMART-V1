package com.example.demo.enums;

/**
 * How a user is allowed to sign in.
 *
 * Stored on the user rather than inferred from "is password_hash null?",
 * because the two can legitimately differ and inferring would hide the case
 * that matters most: an account that has BOTH a password and a linked Google
 * identity.
 */
public enum AuthProvider {

    /** Email + password only. No Google identity attached. */
    LOCAL,

    /** Google only. This account has no password and cannot use /api/auth/login. */
    GOOGLE,

    /**
     * Registered with a password, then later signed in with Google on the same
     * verified email. Either route works.
     */
    BOTH;

    /** True if this account can authenticate with a password. */
    public boolean allowsPasswordLogin() {
        return this == LOCAL || this == BOTH;
    }

    /** True if this account has a Google identity linked. */
    public boolean hasGoogleLinked() {
        return this == GOOGLE || this == BOTH;
    }

    /**
     * What this provider becomes once a Google identity is linked to it.
     * LOCAL gains Google and becomes BOTH; the others are already linked.
     */
    public AuthProvider withGoogleLinked() {
        return this == LOCAL ? BOTH : this;
    }
}
