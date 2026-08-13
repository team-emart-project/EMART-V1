package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything under `emart.email.*` in application.properties.
 *
 * Kept separate from Spring's own `spring.mail.*` (host, port, credentials):
 * those describe HOW to reach the SMTP server, these describe WHAT this
 * service sends and to whom it answers.
 */
@ConfigurationProperties(prefix = "emart.email")
public class EmailProperties {

    /**
     * Master switch. False keeps the endpoint alive and still returns 202, but
     * nothing is handed to SMTP — useful when a demo has no internet.
     */
    private boolean enabled = true;

    /**
     * Renders the message and LOGS the full HTML instead of sending it.
     *
     * This is the mode to develop against: no credentials, no mailbox to
     * check, and the exact body the customer would receive appears in the
     * console. Ignored when enabled=false (nothing is rendered at all).
     */
    private boolean dryRun = false;

    /** The From: address. Gmail rewrites this to the authenticated account. */
    private String from = "noreply@emart.local";

    /** Display name shown next to the From: address. */
    private String fromName = "e-MART";

    /** Prefixed to every subject line, e.g. "[e-MART] Order Placed - ORD-...". */
    private String subjectPrefix = "[e-MART]";

    /** Printed in the email footer so the customer has somewhere to reply. */
    private String supportEmail = "support@emart.local";

    /** Shown in the invoice header. */
    private String storeName = "e-MART";

    /** Prefixed to every amount in the invoice. */
    private String currencySymbol = "₹";

    /**
     * Shared secret both backends must send as X-API-Key.
     *
     * Blank DISABLES the check — convenient for a first local run, and the
     * service logs a warning at startup so it is never silently blank in a
     * deployment that meant to set it.
     */
    private String apiKey = "";

    /** How many times to try SMTP before giving up on a message. */
    private int maxAttempts = 3;

    /** Pause between those attempts. */
    private long retryDelayMs = 2000L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getSubjectPrefix() { return subjectPrefix; }
    public void setSubjectPrefix(String subjectPrefix) { this.subjectPrefix = subjectPrefix; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
}
