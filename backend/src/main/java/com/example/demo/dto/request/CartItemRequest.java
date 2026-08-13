package com.example.demo.dto.request;

import com.example.demo.enums.PriceOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Body for POST /api/cart/items — add a product to the cart.
 *
 * The client sends only WHICH price option was ticked, never the price itself
 * or the number of points. Both are looked up server-side from the catalogue,
 * so editing the request body cannot buy a phone for one point.
 */
public class CartItemRequest {

    @NotNull(message = "prodId is required")
    private Integer prodId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    /**
     * REGULAR | MEMBER | POINTS | HYBRID.
     * Defaults to REGULAR so an omitted field never accidentally spends points.
     */
    private PriceOption priceOption = PriceOption.REGULAR;

    public CartItemRequest() {
    }

    public CartItemRequest(Integer prodId, Integer quantity, PriceOption priceOption) {
        this.prodId = prodId;
        this.quantity = quantity;
        this.priceOption = priceOption;
    }

    public Integer getProdId() {
        return prodId;
    }

    public void setProdId(Integer prodId) {
        this.prodId = prodId;
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

        private Integer prodId;
        private Integer quantity;
        private PriceOption priceOption = PriceOption.REGULAR;

        public Builder prodId(Integer prodId) {
            this.prodId = prodId;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder priceOption(PriceOption priceOption) {
            this.priceOption = priceOption;
            return this;
        }

        public CartItemRequest build() {
            return new CartItemRequest(prodId, quantity, priceOption);
        }
    }
}
