package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Everything under `emart.email-service.*` — how to reach the email microservice. */
@ConfigurationProperties(prefix = "emart.email-service")
public class EmailServiceProperties {

    /**
     * False turns the integration off completely: the order still saves, the
     * listener logs one line and no HTTP call is attempted.
     *
     * Keep it false when the microservice is not running, otherwise every
     * checkout logs a connection-refused warning.
     */
    private boolean enabled = true;

    /** Where the microservice listens. It defaults to port 8082. */
    private String baseUrl = "http://localhost:8082";

    /** Must match emart.email.api-key in the microservice. */
    private String apiKey = "";

    /** Give up on connecting after this many milliseconds. */
    private int connectTimeoutMs = 3000;

    /** Give up waiting for the 202 after this many milliseconds. */
    private int readTimeoutMs = 5000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
