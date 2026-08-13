package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/**
 * What the API returns for one product.
 *
 * PRICE VISIBILITY
 * ----------------
 * mrpPrice is the normal price and is always sent. The three e-MART card
 * offers are only populated for an APPROVED cardholder; for everyone else the
 * service leaves them null and @JsonInclude(NON_NULL) drops them from the JSON
 * entirely. A non-member therefore cannot read member pricing out of the
 * payload — the numbers are not merely hidden by CSS, they are not sent.
 *
 * A null offer field ALSO means "this product does not carry that offer", which
 * is what the product card reads to decide whether to render each checkbox.
 * Both meanings collapse to the same UI behaviour (no checkbox), so one null
 * check covers them.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private Integer prodId;

    private String prodName;

    private String prodShortDesc;

    /** Detail page only; omitted from list responses to keep grids small. */
    private String prodLongDesc;

    /** The normal price. Always present, always the top line on the card. */
    private BigDecimal mrpPrice;

    /** Option 1 — member cash price. Null = not offered / caller is not a member. */
    private BigDecimal cardholderPrice;

    /** mrpPrice - cardholderPrice, precomputed so the UI never does money maths. */
    private BigDecimal cardholderSaving;

    /** Option 2 — e-Points to buy one unit outright; cash charged is 0. */
    private Integer pointsPrice;

    /** Option 3 — the cash half of the combo. */
    private BigDecimal hybridCashPrice;

    /** Option 3 — the points half of the combo. */
    private Integer hybridPoints;

    private String brand;

    private Integer stockQuantity;

    /** stockQuantity > 0, precomputed for the UI. */
    private Boolean inStock;

    private BigDecimal rating;

    private Integer ratingCount;

    private BigDecimal discountPercentage;

    /** Primary thumbnail. */
    private String prodImagePath;

    private Integer catmasterId;

    private String categoryName;

    /** Detail page only. */
    private List<ProductVariantResponse> variants;

    /** Detail page only — a 12-product grid needs 12 thumbnails, not 60 URLs. */
    private List<ProductImageResponse> images;

    public ProductResponse() {
    }

    public ProductResponse(Integer prodId, String prodName, String prodShortDesc, String prodLongDesc,
                           BigDecimal mrpPrice, BigDecimal cardholderPrice, BigDecimal cardholderSaving,
                           Integer pointsPrice, BigDecimal hybridCashPrice, Integer hybridPoints,
                           String brand, Integer stockQuantity, Boolean inStock, BigDecimal rating,
                           Integer ratingCount, BigDecimal discountPercentage, String prodImagePath,
                           Integer catmasterId, String categoryName, List<ProductVariantResponse> variants,
                           List<ProductImageResponse> images) {
        this.prodId = prodId;
        this.prodName = prodName;
        this.prodShortDesc = prodShortDesc;
        this.prodLongDesc = prodLongDesc;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.cardholderSaving = cardholderSaving;
        this.pointsPrice = pointsPrice;
        this.hybridCashPrice = hybridCashPrice;
        this.hybridPoints = hybridPoints;
        this.brand = brand;
        this.stockQuantity = stockQuantity;
        this.inStock = inStock;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.discountPercentage = discountPercentage;
        this.prodImagePath = prodImagePath;
        this.catmasterId = catmasterId;
        this.categoryName = categoryName;
        this.variants = variants;
        this.images = images;
    }

    public Integer getProdId() { return prodId; }
    public void setProdId(Integer prodId) { this.prodId = prodId; }

    public String getProdName() { return prodName; }
    public void setProdName(String prodName) { this.prodName = prodName; }

    public String getProdShortDesc() { return prodShortDesc; }
    public void setProdShortDesc(String prodShortDesc) { this.prodShortDesc = prodShortDesc; }

    public String getProdLongDesc() { return prodLongDesc; }
    public void setProdLongDesc(String prodLongDesc) { this.prodLongDesc = prodLongDesc; }

    public BigDecimal getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; }

    public BigDecimal getCardholderPrice() { return cardholderPrice; }
    public void setCardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; }

    public BigDecimal getCardholderSaving() { return cardholderSaving; }
    public void setCardholderSaving(BigDecimal cardholderSaving) { this.cardholderSaving = cardholderSaving; }

    public Integer getPointsPrice() { return pointsPrice; }
    public void setPointsPrice(Integer pointsPrice) { this.pointsPrice = pointsPrice; }

    public BigDecimal getHybridCashPrice() { return hybridCashPrice; }
    public void setHybridCashPrice(BigDecimal hybridCashPrice) { this.hybridCashPrice = hybridCashPrice; }

    public Integer getHybridPoints() { return hybridPoints; }
    public void setHybridPoints(Integer hybridPoints) { this.hybridPoints = hybridPoints; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }

    public String getProdImagePath() { return prodImagePath; }
    public void setProdImagePath(String prodImagePath) { this.prodImagePath = prodImagePath; }

    public Integer getCatmasterId() { return catmasterId; }
    public void setCatmasterId(Integer catmasterId) { this.catmasterId = catmasterId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public List<ProductVariantResponse> getVariants() { return variants; }
    public void setVariants(List<ProductVariantResponse> variants) { this.variants = variants; }

    public List<ProductImageResponse> getImages() { return images; }
    public void setImages(List<ProductImageResponse> images) { this.images = images; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer prodId;
        private String prodName;
        private String prodShortDesc;
        private String prodLongDesc;
        private BigDecimal mrpPrice;
        private BigDecimal cardholderPrice;
        private BigDecimal cardholderSaving;
        private Integer pointsPrice;
        private BigDecimal hybridCashPrice;
        private Integer hybridPoints;
        private String brand;
        private Integer stockQuantity;
        private Boolean inStock;
        private BigDecimal rating;
        private Integer ratingCount;
        private BigDecimal discountPercentage;
        private String prodImagePath;
        private Integer catmasterId;
        private String categoryName;
        private List<ProductVariantResponse> variants;
        private List<ProductImageResponse> images;

        public Builder prodId(Integer prodId) { this.prodId = prodId; return this; }
        public Builder prodName(String prodName) { this.prodName = prodName; return this; }
        public Builder prodShortDesc(String prodShortDesc) { this.prodShortDesc = prodShortDesc; return this; }
        public Builder prodLongDesc(String prodLongDesc) { this.prodLongDesc = prodLongDesc; return this; }
        public Builder mrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; return this; }
        public Builder cardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; return this; }
        public Builder cardholderSaving(BigDecimal cardholderSaving) { this.cardholderSaving = cardholderSaving; return this; }
        public Builder pointsPrice(Integer pointsPrice) { this.pointsPrice = pointsPrice; return this; }
        public Builder hybridCashPrice(BigDecimal hybridCashPrice) { this.hybridCashPrice = hybridCashPrice; return this; }
        public Builder hybridPoints(Integer hybridPoints) { this.hybridPoints = hybridPoints; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder stockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; return this; }
        public Builder inStock(Boolean inStock) { this.inStock = inStock; return this; }
        public Builder rating(BigDecimal rating) { this.rating = rating; return this; }
        public Builder ratingCount(Integer ratingCount) { this.ratingCount = ratingCount; return this; }
        public Builder discountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; return this; }
        public Builder prodImagePath(String prodImagePath) { this.prodImagePath = prodImagePath; return this; }
        public Builder catmasterId(Integer catmasterId) { this.catmasterId = catmasterId; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder variants(List<ProductVariantResponse> variants) { this.variants = variants; return this; }
        public Builder images(List<ProductImageResponse> images) { this.images = images; return this; }

        public ProductResponse build() {
            return new ProductResponse(prodId, prodName, prodShortDesc, prodLongDesc, mrpPrice,
                    cardholderPrice, cardholderSaving, pointsPrice, hybridCashPrice, hybridPoints, brand,
                    stockQuantity, inStock, rating, ratingCount, discountPercentage, prodImagePath,
                    catmasterId, categoryName, variants, images);
        }
    }
}
