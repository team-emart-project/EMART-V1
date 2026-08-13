package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Body for POST /api/payments/{orderId}/verify.
 *
 * This is a MOCK gateway. A real integration would never accept a raw card
 * number on your own server — it would use a gateway token from the client.
 * Only the last 4 digits are persisted.
 *
 * WHY THE CARD FIELDS ARE NOT @NotBlank
 * -------------------------------------
 * An order paid entirely with e-Points has a total of zero, so there is no card
 * and nothing to charge. Whether a card is required therefore depends on ANOTHER
 * field (the amount), and a per-field annotation cannot express that. The rule
 * lives in PaymentServiceImpl, which knows the order total.
 *
 * The @Pattern rules stay: they ignore null but still reject a malformed card
 * number, so a card that IS supplied is still validated here.
 */
public class PaymentVerifyRequest {

    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13 to 19 digits")
    private String cardNumber;

    private String cardHolderName;

    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Expiry must be in MM/YY format")
    private String expiry;

    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;

    /** Must equal the order total — guards against a tampered client. */
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    public PaymentVerifyRequest() {
    }

    public PaymentVerifyRequest(String cardNumber, String cardHolderName, String expiry,
                                String cvv, BigDecimal amount) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expiry = expiry;
        this.cvv = cvv;
        this.amount = amount;
    }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String cardNumber;
        private String cardHolderName;
        private String expiry;
        private String cvv;
        private BigDecimal amount;
        public Builder cardNumber(String v) { this.cardNumber = v; return this; }
        public Builder cardHolderName(String v) { this.cardHolderName = v; return this; }
        public Builder expiry(String v) { this.expiry = v; return this; }
        public Builder cvv(String v) { this.cvv = v; return this; }
        public Builder amount(BigDecimal v) { this.amount = v; return this; }
        public PaymentVerifyRequest build() {
            return new PaymentVerifyRequest(cardNumber, cardHolderName, expiry, cvv, amount);
        }
    }
}
