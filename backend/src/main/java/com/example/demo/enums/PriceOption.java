package com.example.demo.enums;

/**
 * The four ways a shopper can pay for a single cart line.
 *
 * On the product card this is a set of checkboxes that behave like radio
 * buttons — exactly one applies to any given line. Storing the CHOICE rather
 * than just the resulting number means the cart can re-derive and re-validate
 * the price from the live catalogue at checkout, so a tampered request body
 * cannot inject its own price.
 */
public enum PriceOption {

    /** Normal price. Always available, and the fallback for everything else. */
    REGULAR,

    /** Option 1 — the e-MART card member cash price. */
    MEMBER,

    /** Option 2 — paid entirely in e-Points. Cash charged is 0. */
    POINTS,

    /** Option 3 — part cash, part e-Points (e.g. 800 + 50 e-Points). */
    HYBRID;

    /** True for the options that only an approved cardholder may use. */
    public boolean requiresCardholder() {
        return this != REGULAR;
    }

    /** True for the options that spend e-Points. */
    public boolean spendsPoints() {
        return this == POINTS || this == HYBRID;
    }
}
