package com.example.dto.response;

import java.math.BigDecimal;

/** One line of the cart, with pricing already resolved for this user. */
public class CartItemResponse {

    private Integer cartItemId;
    private Integer prodId;
    private String prodName;
    private String prodImagePath;

    private BigDecimal mrpPrice;
    private BigDecimal cardholderPrice;

    /** Whichever of the two above actually applies to this user. */
    private BigDecimal unitPriceApplied;

    private Integer quantity;

    /** unitPriceApplied * quantity */
    private BigDecimal lineTotal;

    /** mrpPrice * quantity - lineTotal — what the card saved them on this line. */
    private BigDecimal lineSavings;

    private Boolean redeemPoints;
    private Integer pointsUsed;

    /** Ceiling for this line = product.pointsToRedeem * quantity. */
    private Integer maxPointsRedeemable;

    public CartItemResponse() {
    }

    public CartItemResponse(Integer cartItemId, Integer prodId, String prodName, String prodImagePath,
                             BigDecimal mrpPrice, BigDecimal cardholderPrice, BigDecimal unitPriceApplied,
                             Integer quantity, BigDecimal lineTotal, BigDecimal lineSavings,
                             Boolean redeemPoints, Integer pointsUsed, Integer maxPointsRedeemable) {
        this.cartItemId = cartItemId;
        this.prodId = prodId;
        this.prodName = prodName;
        this.prodImagePath = prodImagePath;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.unitPriceApplied = unitPriceApplied;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
        this.lineSavings = lineSavings;
        this.redeemPoints = redeemPoints;
        this.pointsUsed = pointsUsed;
        this.maxPointsRedeemable = maxPointsRedeemable;
    }

    public Integer getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Integer cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Integer getProdId() {
        return prodId;
    }

    public void setProdId(Integer prodId) {
        this.prodId = prodId;
    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public String getProdImagePath() {
        return prodImagePath;
    }

    public void setProdImagePath(String prodImagePath) {
        this.prodImagePath = prodImagePath;
    }

    public BigDecimal getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(BigDecimal mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public BigDecimal getCardholderPrice() {
        return cardholderPrice;
    }

    public void setCardholderPrice(BigDecimal cardholderPrice) {
        this.cardholderPrice = cardholderPrice;
    }

    public BigDecimal getUnitPriceApplied() {
        return unitPriceApplied;
    }

    public void setUnitPriceApplied(BigDecimal unitPriceApplied) {
        this.unitPriceApplied = unitPriceApplied;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getLineSavings() {
        return lineSavings;
    }

    public void setLineSavings(BigDecimal lineSavings) {
        this.lineSavings = lineSavings;
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

    public Integer getMaxPointsRedeemable() {
        return maxPointsRedeemable;
    }

    public void setMaxPointsRedeemable(Integer maxPointsRedeemable) {
        this.maxPointsRedeemable = maxPointsRedeemable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer cartItemId;
        private Integer prodId;
        private String prodName;
        private String prodImagePath;
        private BigDecimal mrpPrice;
        private BigDecimal cardholderPrice;
        private BigDecimal unitPriceApplied;
        private Integer quantity;
        private BigDecimal lineTotal;
        private BigDecimal lineSavings;
        private Boolean redeemPoints;
        private Integer pointsUsed;
        private Integer maxPointsRedeemable;

        public Builder cartItemId(Integer cartItemId) {
            this.cartItemId = cartItemId;
            return this;
        }

        public Builder prodId(Integer prodId) {
            this.prodId = prodId;
            return this;
        }

        public Builder prodName(String prodName) {
            this.prodName = prodName;
            return this;
        }

        public Builder prodImagePath(String prodImagePath) {
            this.prodImagePath = prodImagePath;
            return this;
        }

        public Builder mrpPrice(BigDecimal mrpPrice) {
            this.mrpPrice = mrpPrice;
            return this;
        }

        public Builder cardholderPrice(BigDecimal cardholderPrice) {
            this.cardholderPrice = cardholderPrice;
            return this;
        }

        public Builder unitPriceApplied(BigDecimal unitPriceApplied) {
            this.unitPriceApplied = unitPriceApplied;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder lineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
            return this;
        }

        public Builder lineSavings(BigDecimal lineSavings) {
            this.lineSavings = lineSavings;
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

        public Builder maxPointsRedeemable(Integer maxPointsRedeemable) {
            this.maxPointsRedeemable = maxPointsRedeemable;
            return this;
        }

        public CartItemResponse build() {
            return new CartItemResponse(cartItemId, prodId, prodName, prodImagePath, mrpPrice, cardholderPrice,
                    unitPriceApplied, quantity, lineTotal, lineSavings, redeemPoints, pointsUsed,
                    maxPointsRedeemable);
        }
    }
}
