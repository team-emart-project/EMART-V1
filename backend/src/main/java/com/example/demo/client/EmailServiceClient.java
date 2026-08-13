package com.example.demo.client;

import com.example.demo.client.dto.OrderEmailPayload;
import com.example.demo.config.EmailServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The one place this backend calls backend-email-microservice.
 *
 * IT NEVER THROWS. An order that is already committed must not be reported as
 * a failure because a notification could not be delivered — the customer paid,
 * the stock moved, the row exists. A missing confirmation email is a support
 * problem; a checkout that appears to have failed is a much worse one.
 * Failures are logged and swallowed, and the boolean is there for callers (and
 * tests) that want to know.
 */
@Component
public class EmailServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceClient.class);

    private static final String SEND_ORDER_EMAIL = "/api/send-order-email";

    private final RestClient restClient;
    private final EmailServiceProperties properties;

    public EmailServiceClient(@Qualifier("emailServiceRestClient") RestClient restClient,
                              EmailServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * POSTs an order-placed notification.
     *
     * @return true if the email service accepted it (202). Accepted means
     *         QUEUED there, not delivered — the microservice hands the send to
     *         its own background pool and reports the outcome in its own log.
     */
    public boolean sendOrderPlaced(OrderEmailPayload payload) {

        if (!properties.isEnabled()) {
            log.debug("emart.email-service.enabled=false - not notifying for orderNo={}",
                    payload.order().orderNo());
            return false;
        }

        String orderNo = payload.order().orderNo();

        try {
            restClient.post()
                    .uri(SEND_ORDER_EMAIL)
                    .body(payload)
                    .retrieve()
                    // The response body is a receipt, not something this
                    // backend acts on. toBodilessEntity() still applies the
                    // default 4xx/5xx handling, so a rejection lands in the
                    // catch below instead of passing silently.
                    .toBodilessEntity();

            log.info("Order email requested for orderNo={} via {}", orderNo, properties.getBaseUrl());
            return true;

        } catch (RestClientException ex) {
            // Covers the service being down, a wrong/missing API key (401) and
            // a payload it rejected (400). All three mean the same thing to us:
            // no email, order unaffected.
            log.warn("Email service did not accept orderNo={} ({}). "
                            + "The order is fine; only the confirmation email was not sent.",
                    orderNo, ex.getMessage());
            return false;
        }
    }
}
