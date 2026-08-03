package com.example.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the `product_master` table (from your teacher's original design).
 *
 * Owned by Module 5 (Catalog); defined here because Module 6 needs product
 * pricing and the points-redemption ceiling for every cart line.
 *
 * NOTE: this table has no stock_quantity column, so the cart deliberately
 * performs no stock validation.
 */
@Entity
@Table(name = "product_master")
public class ProductMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_id")
    private Integer prodId;

    /**
     * LAZY so that listing a cart does not silently drag the whole category
     * row back from the database on every line item.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catmaster_id", nullable = false)
    private CategoryMaster category;

    @Column(name = "prod_name", nullable = false, length = 255)
    private String prodName;

    @Column(name = "prod_short_desc", length = 500)
    private String prodShortDesc;

    @Column(name = "prod_long_desc", columnDefinition = "TEXT")
    private String prodLongDesc;

    /** Price a normal (non-cardholder) member pays. */
    @Column(name = "mrp_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal mrpPrice;

    /** Price an e-MART cardholder pays. */
    @Column(name = "cardholder_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal cardholderPrice;

    /** Max e-Points redeemable against ONE unit of this product. 0 = not redeemable. */
    @Column(name = "points_to_redeem", nullable = false)
    private Integer pointsToRedeem;

    @Column(name = "prod_image_path", length = 255)
    private String prodImagePath;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProductMaster() {
    }

    public ProductMaster(Integer prodId, CategoryMaster category, String prodName, String prodShortDesc,
                          String prodLongDesc, BigDecimal mrpPrice, BigDecimal cardholderPrice,
                          Integer pointsToRedeem, String prodImagePath, LocalDateTime createdAt) {
        this.prodId = prodId;
        this.category = category;
        this.prodName = prodName;
        this.prodShortDesc = prodShortDesc;
        this.prodLongDesc = prodLongDesc;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.pointsToRedeem = pointsToRedeem;
        this.prodImagePath = prodImagePath;
        this.createdAt = createdAt;
    }

    public Integer getProdId() {
        return prodId;
    }

    public void setProdId(Integer prodId) {
        this.prodId = prodId;
    }

    public CategoryMaster getCategory() {
        return category;
    }

    public void setCategory(CategoryMaster category) {
        this.category = category;
    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public String getProdShortDesc() {
        return prodShortDesc;
    }

    public void setProdShortDesc(String prodShortDesc) {
        this.prodShortDesc = prodShortDesc;
    }

    public String getProdLongDesc() {
        return prodLongDesc;
    }

    public void setProdLongDesc(String prodLongDesc) {
        this.prodLongDesc = prodLongDesc;
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

    public Integer getPointsToRedeem() {
        return pointsToRedeem;
    }

    public void setPointsToRedeem(Integer pointsToRedeem) {
        this.pointsToRedeem = pointsToRedeem;
    }

    public String getProdImagePath() {
        return prodImagePath;
    }

    public void setProdImagePath(String prodImagePath) {
        this.prodImagePath = prodImagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer prodId;
        private CategoryMaster category;
        private String prodName;
        private String prodShortDesc;
        private String prodLongDesc;
        private BigDecimal mrpPrice;
        private BigDecimal cardholderPrice;
        private Integer pointsToRedeem;
        private String prodImagePath;
        private LocalDateTime createdAt;

        public Builder prodId(Integer prodId) {
            this.prodId = prodId;
            return this;
        }

        public Builder category(CategoryMaster category) {
            this.category = category;
            return this;
        }

        public Builder prodName(String prodName) {
            this.prodName = prodName;
            return this;
        }

        public Builder prodShortDesc(String prodShortDesc) {
            this.prodShortDesc = prodShortDesc;
            return this;
        }

        public Builder prodLongDesc(String prodLongDesc) {
            this.prodLongDesc = prodLongDesc;
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

        public Builder pointsToRedeem(Integer pointsToRedeem) {
            this.pointsToRedeem = pointsToRedeem;
            return this;
        }

        public Builder prodImagePath(String prodImagePath) {
            this.prodImagePath = prodImagePath;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProductMaster build() {
            return new ProductMaster(prodId, category, prodName, prodShortDesc, prodLongDesc, mrpPrice,
                    cardholderPrice, pointsToRedeem, prodImagePath, createdAt);
        }
    }
}
