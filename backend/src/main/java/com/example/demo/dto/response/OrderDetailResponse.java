package com.example.demo.dto.response;

import java.math.BigDecimal;

public class OrderDetailResponse {

    private Integer productId;

    private String productName;

    private Integer quantity;

    private BigDecimal mrpPrice;

    private BigDecimal cardholderPrice;

    private String priceOption;

    private BigDecimal priceCharged;

    private Integer pointsRedeemed;

    public OrderDetailResponse() {
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public String getPriceOption() {
        return priceOption;
    }

    public void setPriceOption(String priceOption) {
        this.priceOption = priceOption;
    }

    public BigDecimal getPriceCharged() {
        return priceCharged;
    }

    public void setPriceCharged(BigDecimal priceCharged) {
        this.priceCharged = priceCharged;
    }

    public Integer getPointsRedeemed() {
        return pointsRedeemed;
    }

    public void setPointsRedeemed(Integer pointsRedeemed) {
        this.pointsRedeemed = pointsRedeemed;
    }

}