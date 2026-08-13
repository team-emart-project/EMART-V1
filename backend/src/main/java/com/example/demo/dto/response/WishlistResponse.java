package com.example.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One wishlist entry, with enough product detail to render a card. */
public class WishlistResponse {

    private Integer wishlistId;
    private Integer prodId;
    private String prodName;
    private String prodShortDesc;
    private String prodImagePath;
    private BigDecimal mrpPrice;
    private BigDecimal cardholderPrice;
    private Integer pointsPrice;
    private LocalDateTime addedAt;

    public WishlistResponse() {
    }

    public WishlistResponse(Integer wishlistId, Integer prodId, String prodName,
                            String prodShortDesc, String prodImagePath, BigDecimal mrpPrice,
                            BigDecimal cardholderPrice, Integer pointsPrice, LocalDateTime addedAt) {
        this.wishlistId = wishlistId;
        this.prodId = prodId;
        this.prodName = prodName;
        this.prodShortDesc = prodShortDesc;
        this.prodImagePath = prodImagePath;
        this.mrpPrice = mrpPrice;
        this.cardholderPrice = cardholderPrice;
        this.pointsPrice = pointsPrice;
        this.addedAt = addedAt;
    }

    public Integer getWishlistId() { return wishlistId; }
    public void setWishlistId(Integer wishlistId) { this.wishlistId = wishlistId; }
    public Integer getProdId() { return prodId; }
    public void setProdId(Integer prodId) { this.prodId = prodId; }
    public String getProdName() { return prodName; }
    public void setProdName(String prodName) { this.prodName = prodName; }
    public String getProdShortDesc() { return prodShortDesc; }
    public void setProdShortDesc(String prodShortDesc) { this.prodShortDesc = prodShortDesc; }
    public String getProdImagePath() { return prodImagePath; }
    public void setProdImagePath(String prodImagePath) { this.prodImagePath = prodImagePath; }
    public BigDecimal getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(BigDecimal mrpPrice) { this.mrpPrice = mrpPrice; }
    public BigDecimal getCardholderPrice() { return cardholderPrice; }
    public void setCardholderPrice(BigDecimal cardholderPrice) { this.cardholderPrice = cardholderPrice; }
    public Integer getPointsPrice() { return pointsPrice; }
    public void setPointsPrice(Integer pointsPrice) { this.pointsPrice = pointsPrice; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer wishlistId;
        private Integer prodId;
        private String prodName;
        private String prodShortDesc;
        private String prodImagePath;
        private BigDecimal mrpPrice;
        private BigDecimal cardholderPrice;
        private Integer pointsPrice;
        private LocalDateTime addedAt;

        public Builder wishlistId(Integer v) { this.wishlistId = v; return this; }
        public Builder prodId(Integer v) { this.prodId = v; return this; }
        public Builder prodName(String v) { this.prodName = v; return this; }
        public Builder prodShortDesc(String v) { this.prodShortDesc = v; return this; }
        public Builder prodImagePath(String v) { this.prodImagePath = v; return this; }
        public Builder mrpPrice(BigDecimal v) { this.mrpPrice = v; return this; }
        public Builder cardholderPrice(BigDecimal v) { this.cardholderPrice = v; return this; }
        public Builder pointsPrice(Integer v) { this.pointsPrice = v; return this; }
        public Builder addedAt(LocalDateTime v) { this.addedAt = v; return this; }

        public WishlistResponse build() {
            return new WishlistResponse(wishlistId, prodId, prodName, prodShortDesc,
                    prodImagePath, mrpPrice, cardholderPrice, pointsPrice, addedAt);
        }
    }
}
