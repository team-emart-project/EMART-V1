package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * What the caller gets back from /api/send-order-email.
 *
 * status is ACCEPTED, not SENT. The HTTP response is written before SMTP is
 * even contacted, so claiming "SENT" here would be a lie the caller might act
 * on. Delivery is reported in this service's own log, keyed by requestId.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailSendResponse(

        /** Correlation id. Grep the service log for it to trace one message. */
        String requestId,

        String orderNo,

        /** Masked — a response body can end up in the caller's log too. */
        String recipient,

        /** ACCEPTED | SKIPPED (emart.email.enabled=false). */
        String status,

        LocalDateTime acceptedAt

) {
    public static EmailSendResponse accepted(String requestId, String orderNo, String maskedRecipient) {
        return new EmailSendResponse(requestId, orderNo, maskedRecipient, "ACCEPTED", LocalDateTime.now());
    }

    public static EmailSendResponse skipped(String requestId, String orderNo, String maskedRecipient) {
        return new EmailSendResponse(requestId, orderNo, maskedRecipient, "SKIPPED", LocalDateTime.now());
    }
}
