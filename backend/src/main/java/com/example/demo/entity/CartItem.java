package com.example.demo.entity;

import com.example.demo.enums.PriceOption;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Maps the `cart_items` table — one row per distinct product+price-option in a
 * cart. Adding the same product at the same price option increments
 * {@code quantity} instead of inserting a second row.
 *
 * The chosen {@link PriceOption} is stored per LINE, not per cart, because a
 * shopper can legitimately pay points for a cheap item and cash for an
 * expensive one in the same order.
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

    /**
     * Which of the four price options the shopper ticked.
     * STRING, not ORDINAL: storing 0/1/2/3 would mean that reordering the enum
     * silently reprices every historical row.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_option", nullable = false, length = 10)
    private PriceOption priceOption;

    /**
     * e-Points this line will spend in total (all units). Derived from
     * priceOption and the product's offer, recomputed by the service on every
     * write — never taken from the request body.
     */
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public CartItem() {
    }

    public CartItem(Integer cartItemId, Cart cart, ProductMaster product, Integer quantity,
                    PriceOption priceOption, Integer pointsUsed, LocalDateTime addedAt) {
        this.cartItemId = cartItemId;
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.priceOption = priceOption;
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

    public PriceOption getPriceOption() {
        return priceOption;
    }

    public void setPriceOption(PriceOption priceOption) {
        this.priceOption = priceOption;
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
        private PriceOption priceOption;
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

        public Builder priceOption(PriceOption priceOption) {
            this.priceOption = priceOption;
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
            return new CartItem(cartItemId, cart, product, quantity, priceOption, pointsUsed, addedAt);
        }
    }
}
