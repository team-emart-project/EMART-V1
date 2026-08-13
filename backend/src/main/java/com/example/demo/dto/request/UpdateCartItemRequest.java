package com.example.demo.dto.request;

import com.example.demo.enums.PriceOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Body for PUT /api/cart/items/{cartItemId} — change quantity, or switch the
 * line to a different price option.
 *
 * priceOption may be omitted, in which case the line keeps whatever option it
 * already had. That lets the quantity stepper in the cart send just a quantity
 * without having to re-state the pricing choice.
 */
public class UpdateCartItemRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    /** Null means "leave the existing price option alone". */
    private PriceOption priceOption;

    public UpdateCartItemRequest() {
    }

    public UpdateCartItemRequest(Integer quantity, PriceOption priceOption) {
        this.quantity = quantity;
        this.priceOption = priceOption;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public PriceOption getPriceOption() {
        return priceOption;
    }

    public void setPriceOption(PriceOption priceOption) {
        this.priceOption = priceOption;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer quantity;
        private PriceOption priceOption;

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder priceOption(PriceOption priceOption) {
            this.priceOption = priceOption;
            return this;
        }

        public UpdateCartItemRequest build() {
            return new UpdateCartItemRequest(quantity, priceOption);
        }
    }
}
