package com.example.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body for POST /api/cart/items — add a product to the cart. */
public class CartItemRequest {

    @NotNull(message = "prodId is required")
    private Integer prodId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    /** Defaults to false so an omitted flag never accidentally spends points. */
    private Boolean redeemPoints = false;

    @PositiveOrZero(message = "pointsUsed cannot be negative")
    private Integer pointsUsed = 0;

    public CartItemRequest() {
    }

    public CartItemRequest(Integer prodId, Integer quantity, Boolean redeemPoints, Integer pointsUsed) {
        this.prodId = prodId;
        this.quantity = quantity;
        this.redeemPoints = redeemPoints;
        this.pointsUsed = pointsUsed;
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

    public Boolean getRedeemPoints() {
        return redeemPoints;
    }

    public void setRedeemPoints(Boolean redeemPoints) {
        this.redeemPoints = redeemPoints;
    }

    public Integer getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(Integer pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer prodId;
        private Integer quantity;
        private Boolean redeemPoints = false;
        private Integer pointsUsed = 0;

        public Builder prodId(Integer prodId) {
            this.prodId = prodId;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder redeemPoints(Boolean redeemPoints) {
            this.redeemPoints = redeemPoints;
            return this;
        }

        public Builder pointsUsed(Integer pointsUsed) {
            this.pointsUsed = pointsUsed;
            return this;
        }

        public CartItemRequest build() {
            return new CartItemRequest(prodId, quantity, redeemPoints, pointsUsed);
        }
    }
}
