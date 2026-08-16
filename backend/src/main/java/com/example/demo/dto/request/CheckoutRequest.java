package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Body for checkout-preview and place-order.
 *
 * Delivery is courier-only in this phase, so there is no store-pickup option
 * and no storeId — the schema has no store table.
 */
public class CheckoutRequest {

    @NotNull(message = "shippingAddressId is required")
    private Integer shippingAddressId;

    /** Optional: defaults to the shipping address when omitted. */
    private Integer billingAddressId;

    public CheckoutRequest() {
    }

    public CheckoutRequest(Integer shippingAddressId, Integer billingAddressId) {
        this.shippingAddressId = shippingAddressId;
        this.billingAddressId = billingAddressId;
    }

    public Integer getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(Integer shippingAddressId) { this.shippingAddressId = shippingAddressId; }
    public Integer getBillingAddressId() { return billingAddressId; }
    public void setBillingAddressId(Integer billingAddressId) { this.billingAddressId = billingAddressId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer shippingAddressId;
        private Integer billingAddressId;
        public Builder shippingAddressId(Integer v) { this.shippingAddressId = v; return this; }
        public Builder billingAddressId(Integer v) { this.billingAddressId = v; return this; }
        public CheckoutRequest build() { return new CheckoutRequest(shippingAddressId, billingAddressId); }
    }
}
