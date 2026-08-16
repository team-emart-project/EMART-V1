package com.example.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the `product_master` table (from your teacher's original design).
 *
 * PRICING MODEL
 * -------------
 * {@code mrpPrice} is the normal price and is always present. The other three
 * offers are e-MART card options and are each NULLABLE, because a product may
 * carry any, all, or none of them:
 *
 *   cardholderPrice ......... Option 1  member cash price
 *   pointsPrice ............. Option 2  buy with e-Points only, cash = 0
 *   hybridCashPrice
 *   + hybridPoints .......... Option 3  part cash, part e-Points
 *
 * NULL is meaningful here: it is what the product card reads to decide whether
 * to render that checkbox at all. A product with all three NULL shows only the
 * normal price and "Add to Cart".
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

    /** The normal price. Every product has one; anyone can buy at this price. */
    @Column(name = "mrp_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal mrpPrice;

    /** Option 1 — member cash price. NULL means this product has no member offer. */
    @Column(name = "cardholder_price", precision = 10, scale = 2)
    private BigDecimal cardholderPrice;

    /** Option 2 — total e-Points to buy one unit outright. NULL means not offered. */
    @Column(name = "points_price")
    private Integer pointsPrice;

    /** Option 3 — the cash half of the combo price. NULL means not offered. */
    @Column(name = "hybrid_cash_price", precision = 10, scale = 2)
    private BigDecimal hybridCashPrice;

    /** Option 3 — the points half of the combo price. Set together with the cash half. */
    @Column(name = "hybrid_points")
    private Integer hybridPoints;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    /** 0.0 - 5.0 */
    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "prod_image_path", length = 255)
    private String prodImagePath;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProductMaster() {
    }

    public ProductMaster(Integer prodId, CategoryMaster category, String prodName, String prodShortDesc,
                         String prodLongDesc, BigDecimal mrpPrice, BigDecimal cardholderPrice,
                         Integer pointsPrice, BigDecimal hybridCashPrice, Integer hybridPoints,
                         String brand, Integer stockQuantity,
                         BigDecimal rating, Integer ratingCount, BigDecimal discountPercentage,
                         String prodImagePath, LocalDateTime createdAt) {
        this.prodId = prodId;
        this.category = category;
        this.prodName = prodName;
        this.prodShortDesc = prodShortDesc;
        this.prodLongDesc = prodLongDesc;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.pointsPrice = pointsPrice;
        this.hybridCashPrice = hybridCashPrice;
        this.hybridPoints = hybridPoints;
        this.brand = brand;
        this.stockQuantity = stockQuantity;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.discountPercentage = discountPercentage;
        this.prodImagePath = prodImagePath;
        this.createdAt = createdAt;
    }

    // ------------------------------------------------------------------
    // Offer-availability helpers.
    //
    // These live on the entity so that "does this product offer X?" is
    // answered in exactly one place. The cart, the mapper and the product
    // card all ask the same question, and a null check repeated in three
    // files is a null check that will eventually disagree with itself.
    // ------------------------------------------------------------------

    /** True if Option 1 (member cash price) is offered on this product. */
    public boolean hasMemberOffer() {
        return cardholderPrice != null;
    }

    /** True if Option 2 (points only) is offered on this product. */
    public boolean hasPointsOffer() {
        return pointsPrice != null && pointsPrice > 0;
    }

    /** True if Option 3 (part cash, part points) is offered on this product. */
    public boolean hasHybridOffer() {
        return hybridCashPrice != null && hybridPoints != null && hybridPoints > 0;
    }

    /** True if the product carries any e-MART card offer at all. */
    public boolean hasAnyMemberOffer() {
        return hasMemberOffer() || hasPointsOffer() || hasHybridOffer();
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

    public Integer getPointsPrice() {
        return pointsPrice;
    }

    public void setPointsPrice(Integer pointsPrice) {
        this.pointsPrice = pointsPrice;
    }

    public BigDecimal getHybridCashPrice() {
        return hybridCashPrice;
    }

    public void setHybridCashPrice(BigDecimal hybridCashPrice) {
        this.hybridCashPrice = hybridCashPrice;
    }

    public Integer getHybridPoints() {
        return hybridPoints;
    }

    public void setHybridPoints(Integer hybridPoints) {
        this.hybridPoints = hybridPoints;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }

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
        private Integer pointsPrice;
        private BigDecimal hybridCashPrice;
        private Integer hybridPoints;
        private String brand;
        private Integer stockQuantity;
        private BigDecimal rating;
        private Integer ratingCount;
        private BigDecimal discountPercentage;
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

        public Builder pointsPrice(Integer pointsPrice) {
            this.pointsPrice = pointsPrice;
            return this;
        }

        public Builder hybridCashPrice(BigDecimal hybridCashPrice) {
            this.hybridCashPrice = hybridCashPrice;
            return this;
        }

        public Builder hybridPoints(Integer hybridPoints) {
            this.hybridPoints = hybridPoints;
            return this;
        }

        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder stockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; return this; }
        public Builder rating(BigDecimal rating) { this.rating = rating; return this; }
        public Builder ratingCount(Integer ratingCount) { this.ratingCount = ratingCount; return this; }
        public Builder discountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; return this; }

        public Builder prodImagePath(String prodImagePath) {
            this.prodImagePath = prodImagePath;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProductMaster build() {
            return new ProductMaster(prodId, category, prodName, prodShortDesc, prodLongDesc,
                    mrpPrice, cardholderPrice, pointsPrice, hybridCashPrice, hybridPoints,
                    brand, stockQuantity, rating, ratingCount,
                    discountPercentage, prodImagePath, createdAt);
        }
    }
}
