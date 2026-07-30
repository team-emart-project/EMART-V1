package com.example.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body for PUT /api/cart/items/{cartItemId} — set quantity / points on a line. */
public class UpdateCartItemRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private Boolean redeemPoints = false;

    @PositiveOrZero(message = "pointsUsed cannot be negative")
    private Integer pointsUsed = 0;

    public UpdateCartItemRequest() {
    }

    public UpdateCartItemRequest(Integer quantity, Boolean redeemPoints, Integer pointsUsed) {
        this.quantity = quantity;
        this.redeemPoints = redeemPoints;
        this.pointsUsed = pointsUsed;
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
        private Integer quantity;
        private Boolean redeemPoints = false;
        private Integer pointsUsed = 0;

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

        public UpdateCartItemRequest build() {
            return new UpdateCartItemRequest(quantity, redeemPoints, pointsUsed);
        }
    }
}
