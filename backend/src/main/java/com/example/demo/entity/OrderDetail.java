package com.example.demo.entity;

import com.example.demo.enums.PriceOption;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Maps the `order_details` table — one line per product on a placed order.
 *
 * The prices and the product NAME are SNAPSHOTS taken at order time. If the
 * catalog is edited later, a two-year-old invoice must still show what the
 * customer actually saw and paid.
 */
@Entity
@Table(name = "order_details")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_dtl_id")
    private Integer orderDtlId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false)
    private ProductMaster product;

    @Column(name = "prod_name_snapshot", nullable = false, length = 255)
    private String prodNameSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "mrp_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal mrpPrice;

    /** Snapshot of the member price; NULL if the product had no member offer. */
    @Column(name = "cardholder_price", precision = 10, scale = 2)
    private BigDecimal cardholderPrice;

    /** Which price option the shopper bought this line under. */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_option", nullable = false, length = 10)
    private PriceOption priceOption;

    /** Cash actually charged for one unit of this line. 0 when paid in points. */
    @Column(name = "price_charged", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceCharged;

    @Column(name = "points_redeemed", nullable = false)
    private Integer pointsRedeemed;

    public OrderDetail() {
    }

    public OrderDetail(Integer orderDtlId, Orders order, ProductMaster product,
                       String prodNameSnapshot, Integer quantity, BigDecimal mrpPrice,
                       BigDecimal cardholderPrice, PriceOption priceOption,
                       BigDecimal priceCharged, Integer pointsRedeemed) {
        this.orderDtlId = orderDtlId;
        this.order = order;
        this.product = product;
        this.prodNameSnapshot = prodNameSnapshot;
        this.quantity = quantity;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.priceOption = priceOption;
        this.priceCharged = priceCharged;
        this.pointsRedeemed = pointsRedeemed;
    }

    public Integer getOrderDtlId() { return orderDtlId; }
    public void setOrderDtlId(Integer orderDtlId) { this.orderDtlId = orderDtlId; }

    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }

    public ProductMaster getProduct() { return product; }
    public void setProduct(ProductMaster product) { this.product = product; }

    public String getProdNameSnapshot() { return prodNameSnapshot; }
    public void setProdNameSnapshot(String prodNameSnapshot) { this.prodNameSnapshot = prodNameSnapshot; }

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

    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer orderDtlId;
        private Orders order;
        private ProductMaster product;
        private String prodNameSnapshot;
        private Integer quantity;
        private BigDecimal mrpPrice;
        private BigDecimal cardholderPrice;
        private PriceOption priceOption;
        private BigDecimal priceCharged;
        private Integer pointsRedeemed;

        public Builder orderDtlId(Integer orderDtlId) { this.orderDtlId = orderDtlId; return this; }
        public Builder order(Orders order) { this.order = order; return this; }
        public Builder product(ProductMaster product) { this.product = product; return this; }
        public Builder prodNameSnapshot(String prodNameSnapshot) { this.prodNameSnapshot = prodNameSnapshot; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder mrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; return this; }
        public Builder cardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; return this; }
        public Builder priceOption(PriceOption priceOption) { this.priceOption = priceOption; return this; }
        public Builder priceCharged(BigDecimal priceCharged) { this.priceCharged = priceCharged; return this; }
        public Builder pointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; return this; }

        public OrderDetail build() {
            return new OrderDetail(orderDtlId, order, product, prodNameSnapshot, quantity,
                    mrpPrice, cardholderPrice, priceOption, priceCharged, pointsRedeemed);
        }
    }
}
