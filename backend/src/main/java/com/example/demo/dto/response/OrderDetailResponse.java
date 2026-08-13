package com.example.demo.dto.response;

import com.example.demo.enums.PriceOption;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * One line of a placed order. Every figure here is a SNAPSHOT taken when the
 * order was placed, so a two-year-old invoice still shows what the customer
 * actually saw and paid even if the catalogue has since changed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailResponse {

    private Integer orderDtlId;

    private Integer prodId;

    /** Snapshot of the name at order time, not the live one. */
    private String prodName;

    private Integer quantity;

    private BigDecimal mrpPrice;

    /** Null if the product carried no member offer. */
    private BigDecimal cardholderPrice;

    /** Which option this line was bought under. */
    private PriceOption priceOption;

    /** Cash per unit. Zero when the line was paid in points. */
    private BigDecimal priceCharged;

    /** priceCharged * quantity. */
    private BigDecimal lineTotal;

    /** (mrpPrice - priceCharged) * quantity. */
    private BigDecimal lineSavings;

    /** e-Points spent on this line in total. */
    private Integer pointsRedeemed;

    public OrderDetailResponse() {
    }

    public OrderDetailResponse(Integer orderDtlId, Integer prodId, String prodName, Integer quantity,
                               BigDecimal mrpPrice, BigDecimal cardholderPrice, PriceOption priceOption,
                               BigDecimal priceCharged, BigDecimal lineTotal, BigDecimal lineSavings,
                               Integer pointsRedeemed) {
        this.orderDtlId = orderDtlId;
        this.prodId = prodId;
        this.prodName = prodName;
        this.quantity = quantity;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.priceOption = priceOption;
        this.priceCharged = priceCharged;
        this.lineTotal = lineTotal;
        this.lineSavings = lineSavings;
        this.pointsRedeemed = pointsRedeemed;
    }

    public Integer getOrderDtlId() { return orderDtlId; }
    public void setOrderDtlId(Integer orderDtlId) { this.orderDtlId = orderDtlId; }

    public Integer getProdId() { return prodId; }
    public void setProdId(Integer prodId) { this.prodId = prodId; }

    public String getProdName() { return prodName; }
    public void setProdName(String prodName) { this.prodName = prodName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; }

    public BigDecimal getCardholderPrice() { return cardholderPrice; }
    public void setCardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; }

    public PriceOption getPriceOption() { return priceOption; }
    public void setPriceOption(PriceOption priceOption) { this.priceOption = priceOption; }

    public BigDecimal getPriceCharged() { return priceCharged; }
    public void setPriceCharged(BigDecimal priceCharged) { this.priceCharged = priceCharged; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public BigDecimal getLineSavings() { return lineSavings; }
    public void setLineSavings(BigDecimal lineSavings) { this.lineSavings = lineSavings; }

    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer orderDtlId;
        private Integer prodId;
        private String prodName;
        private Integer quantity;
        private BigDecimal mrpPrice;
        private BigDecimal cardholderPrice;
        private PriceOption priceOption;
        private BigDecimal priceCharged;
        private BigDecimal lineTotal;
        private BigDecimal lineSavings;
        private Integer pointsRedeemed;

        public Builder orderDtlId(Integer orderDtlId) { this.orderDtlId = orderDtlId; return this; }
        public Builder prodId(Integer prodId) { this.prodId = prodId; return this; }
        public Builder prodName(String prodName) { this.prodName = prodName; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder mrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; return this; }
        public Builder cardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; return this; }
        public Builder priceOption(PriceOption priceOption) { this.priceOption = priceOption; return this; }
        public Builder priceCharged(BigDecimal priceCharged) { this.priceCharged = priceCharged; return this; }
        public Builder lineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; return this; }
        public Builder lineSavings(BigDecimal lineSavings) { this.lineSavings = lineSavings; return this; }
        public Builder pointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; return this; }

        public OrderDetailResponse build() {
            return new OrderDetailResponse(orderDtlId, prodId, prodName, quantity, mrpPrice,
                    cardholderPrice, priceOption, priceCharged, lineTotal, lineSavings, pointsRedeemed);
        }
    }
}
