package com.example.demo.dto.response;

import com.example.demo.enums.PriceOption;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * One line in the cart, with its price already resolved.
 *
 * unitPriceApplied is the single number the UI should display and the only one
 * that feeds the total. It is derived server-side from priceOption, so the
 * front end never has to know the pricing rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {

    private Integer cartItemId;

    private Integer prodId;

    private String prodName;

    private String prodImagePath;

    /** Normal price, shown struck through when a cheaper option is active. */
    private BigDecimal mrpPrice;

    /** Option 1. Null if not offered, or caller is not a member. */
    private BigDecimal cardholderPrice;

    /** Option 2. Null if not offered, or caller is not a member. */
    private Integer pointsPrice;

    /** Option 3, cash half. */
    private BigDecimal hybridCashPrice;

    /** Option 3, points half. */
    private Integer hybridPoints;

    /** REGULAR | MEMBER | POINTS | HYBRID — what the shopper ticked. */
    private PriceOption priceOption;

    /** Cash per unit under the chosen option. 0 for POINTS. */
    private BigDecimal unitPriceApplied;

    /** e-Points per unit under the chosen option. 0 for REGULAR/MEMBER. */
    private Integer unitPointsApplied;

    private Integer quantity;

    /** unitPriceApplied * quantity. */
    private BigDecimal lineTotal;

    /** (mrpPrice - unitPriceApplied) * quantity. */
    private BigDecimal lineSavings;

    /** unitPointsApplied * quantity. */
    private Integer pointsUsed;

    public CartItemResponse() {
    }

    public CartItemResponse(Integer cartItemId, Integer prodId, String prodName, String prodImagePath,
                            BigDecimal mrpPrice, BigDecimal cardholderPrice, Integer pointsPrice,
                            BigDecimal hybridCashPrice, Integer hybridPoints, PriceOption priceOption,
                            BigDecimal unitPriceApplied, Integer unitPointsApplied, Integer quantity,
                            BigDecimal lineTotal, BigDecimal lineSavings, Integer pointsUsed) {
        this.cartItemId = cartItemId;
        this.prodId = prodId;
        this.prodName = prodName;
        this.prodImagePath = prodImagePath;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.pointsPrice = pointsPrice;
        this.hybridCashPrice = hybridCashPrice;
        this.hybridPoints = hybridPoints;
        this.priceOption = priceOption;
        this.unitPriceApplied = unitPriceApplied;
        this.unitPointsApplied = unitPointsApplied;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
        this.lineSavings = lineSavings;
        this.pointsUsed = pointsUsed;
    }

    public Integer getCartItemId() { return cartItemId; }
    public void setCartItemId(Integer cartItemId) { this.cartItemId = cartItemId; }

    public Integer getProdId() { return prodId; }
    public void setProdId(Integer prodId) { this.prodId = prodId; }

    public String getProdName() { return prodName; }
    public void setProdName(String prodName) { this.prodName = prodName; }

    public String getProdImagePath() { return prodImagePath; }
    public void setProdImagePath(String prodImagePath) { this.prodImagePath = prodImagePath; }

    public BigDecimal getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; }

    public BigDecimal getCardholderPrice() { return cardholderPrice; }
    public void setCardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; }

    public Integer getPointsPrice() { return pointsPrice; }
    public void setPointsPrice(Integer pointsPrice) { this.pointsPrice = pointsPrice; }

    public BigDecimal getHybridCashPrice() { return hybridCashPrice; }
    public void setHybridCashPrice(BigDecimal hybridCashPrice) { this.hybridCashPrice = hybridCashPrice; }

    public Integer getHybridPoints() { return hybridPoints; }
    public void setHybridPoints(Integer hybridPoints) { this.hybridPoints = hybridPoints; }

    public PriceOption getPriceOption() { return priceOption; }
    public void setPriceOption(PriceOption priceOption) { this.priceOption = priceOption; }

    public BigDecimal getUnitPriceApplied() { return unitPriceApplied; }
    public void setUnitPriceApplied(BigDecimal unitPriceApplied) { this.unitPriceApplied = unitPriceApplied; }

    public Integer getUnitPointsApplied() { return unitPointsApplied; }
    public void setUnitPointsApplied(Integer unitPointsApplied) { this.unitPointsApplied = unitPointsApplied; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public BigDecimal getLineSavings() { return lineSavings; }
    public void setLineSavings(BigDecimal lineSavings) { this.lineSavings = lineSavings; }

    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; }

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
        private Integer pointsPrice;
        private BigDecimal hybridCashPrice;
        private Integer hybridPoints;
        private PriceOption priceOption;
        private BigDecimal unitPriceApplied;
        private Integer unitPointsApplied;
        private Integer quantity;
        private BigDecimal lineTotal;
        private BigDecimal lineSavings;
        private Integer pointsUsed;

        public Builder cartItemId(Integer cartItemId) { this.cartItemId = cartItemId; return this; }
        public Builder prodId(Integer prodId) { this.prodId = prodId; return this; }
        public Builder prodName(String prodName) { this.prodName = prodName; return this; }
        public Builder prodImagePath(String prodImagePath) { this.prodImagePath = prodImagePath; return this; }
        public Builder mrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; return this; }
        public Builder cardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; return this; }
        public Builder pointsPrice(Integer pointsPrice) { this.pointsPrice = pointsPrice; return this; }
        public Builder hybridCashPrice(BigDecimal hybridCashPrice) { this.hybridCashPrice = hybridCashPrice; return this; }
        public Builder hybridPoints(Integer hybridPoints) { this.hybridPoints = hybridPoints; return this; }
        public Builder priceOption(PriceOption priceOption) { this.priceOption = priceOption; return this; }
        public Builder unitPriceApplied(BigDecimal unitPriceApplied) { this.unitPriceApplied = unitPriceApplied; return this; }
        public Builder unitPointsApplied(Integer unitPointsApplied) { this.unitPointsApplied = unitPointsApplied; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder lineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; return this; }
        public Builder lineSavings(BigDecimal lineSavings) { this.lineSavings = lineSavings; return this; }
        public Builder pointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; return this; }

        public CartItemResponse build() {
            return new CartItemResponse(cartItemId, prodId, prodName, prodImagePath, mrpPrice,
                    cardholderPrice, pointsPrice, hybridCashPrice, hybridPoints, priceOption,
                    unitPriceApplied, unitPointsApplied, quantity, lineTotal, lineSavings, pointsUsed);
        }
    }
}
