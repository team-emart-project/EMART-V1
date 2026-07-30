package com.example.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Maps the `cart_items` table — one row per distinct product in a cart.
 * Adding the same product twice increments {@code quantity} instead of
 * inserting a second row.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Integer cartItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false)
    private ProductMaster product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Whether the customer chose to pay part of this line with e-Points. */
    @Column(name = "redeem_points", nullable = false)
    private Boolean redeemPoints;

    /** How many e-Points are applied to this line in total (all units). */
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public CartItem() {
    }

    public CartItem(Integer cartItemId, Cart cart, ProductMaster product, Integer quantity,
                     Boolean redeemPoints, Integer pointsUsed, LocalDateTime addedAt) {
        this.cartItemId = cartItemId;
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.redeemPoints = redeemPoints;
        this.pointsUsed = pointsUsed;
        this.addedAt = addedAt;
    }

    public Integer getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Integer cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public ProductMaster getProduct() {
        return product;
    }

    public void setProduct(ProductMaster product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getRedeemPoints() {
        return redeemPoints;
    }

    public void setRedeemPoints(Boolean redeemPoints) {
        this.redeemPoints = redeemPoints;
    }

    public Integer getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(Integer pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer cartItemId;
        private Cart cart;
        private ProductMaster product;
        private Integer quantity;
        private Boolean redeemPoints;
        private Integer pointsUsed;
        private LocalDateTime addedAt;

        public Builder cartItemId(Integer cartItemId) {
            this.cartItemId = cartItemId;
            return this;
        }

        public Builder cart(Cart cart) {
            this.cart = cart;
            return this;
        }

        public Builder product(ProductMaster product) {
            this.product = product;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder redeemPoints(Boolean redeemPoints) {
            this.redeemPoints = redeemPoints;
            return this;
        }

        public Builder pointsUsed(Integer pointsUsed) {
            this.pointsUsed = pointsUsed;
            return this;
        }

        public Builder addedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public CartItem build() {
            return new CartItem(cartItemId, cart, product, quantity, redeemPoints, pointsUsed, addedAt);
        }
    }
}
