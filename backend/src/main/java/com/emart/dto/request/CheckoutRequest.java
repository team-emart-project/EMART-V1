package com.emart.dto.request;

import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {

    @NotNull(message = "Shipping Address is required")
    private Integer shippingAddressId;

    private Integer billingAddressId;

    public CheckoutRequest() {
    }

    public Integer getShippingAddressId() {
        return shippingAddressId;
    }

    public void setShippingAddressId(Integer shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }

    public Integer getBillingAddressId() {
        return billingAddressId;
    }

    public void setBillingAddressId(Integer billingAddressId) {
        this.billingAddressId = billingAddressId;
    }

}