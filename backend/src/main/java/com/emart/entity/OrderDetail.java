package com.emart.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "order_details")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_dtl_id")
    private Integer orderDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_id", nullable = false)
    private ProductMaster product;

    @Column(name = "prod_name_snapshot", nullable = false)
    private String prodNameSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "mrp_price", nullable = false)
    private BigDecimal mrpPrice;

    @Column(name = "cardholder_price")
    private BigDecimal cardholderPrice;

    @Column(name = "price_option", nullable = false)
    private String priceOption;

    @Column(name = "price_charged", nullable = false)
    private BigDecimal priceCharged;

    @Column(name = "points_redeemed")
    private Integer pointsRedeemed;

    public OrderDetail() {
    }

    // Getters and Setters

    public Integer getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(Integer orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public Orders getOrder() {
        return order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public ProductMaster getProduct() {
        return product;
    }

    public void setProduct(ProductMaster product) {
        this.product = product;
    }

    public String getProdNameSnapshot() {
        return prodNameSnapshot;
    }

    public void setProdNameSnapshot(String prodNameSnapshot) {
        this.prodNameSnapshot = prodNameSnapshot;
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