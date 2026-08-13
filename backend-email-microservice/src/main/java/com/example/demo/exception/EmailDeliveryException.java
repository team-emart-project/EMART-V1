package com.example.demo.exception;

/**
 * SMTP refused the message, or the template could not be rendered.
 *
 * Raised on the background thread, so it does NOT reach the caller — by the
 * time it happens the 202 has already been written. It exists so the failure
 * is a typed, logged event with the orderNo attached rather than a bare
 * MailException three frames deep.
 */
public class EmailDeliveryException extends RuntimeException {

    private final String orderNo;

    public EmailDeliveryException(String orderNo, String message, Throwable cause) {
        super(message, cause);
        this.orderNo = orderNo;
    }

    public EmailDeliveryException(String orderNo, String message) {
        super(message);
        this.orderNo = orderNo;
    }

    public String getOrderNo() {
        return orderNo;
    }
}
