package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body for POST /api/wishlist. */
public class WishlistRequest {

    @NotNull(message = "prodId is required")
    private Integer prodId;

    public WishlistRequest() {
    }

    public WishlistRequest(Integer prodId) {
        this.prodId = prodId;
    }

    public Integer getProdId() { return prodId; }
    public void setProdId(Integer prodId) { this.prodId = prodId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer prodId;
        public Builder prodId(Integer prodId) { this.prodId = prodId; return this; }
        public WishlistRequest build() { return new WishlistRequest(prodId); }
    }
}
