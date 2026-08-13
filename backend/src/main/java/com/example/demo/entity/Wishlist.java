package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Maps the `wishlist` table.
 *
 * Flat design: one row per (user, product). The table carries a UNIQUE
 * constraint on that pair, so a product cannot be wishlisted twice.
 */
@Entity
@Table(name = "wishlist",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "prod_id"}))
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Integer wishlistId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false)
    private ProductMaster product;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public Wishlist() {
    }

    public Wishlist(Integer wishlistId, User user, ProductMaster product, LocalDateTime addedAt) {
        this.wishlistId = wishlistId;
        this.user = user;
        this.product = product;
        this.addedAt = addedAt;
    }

    public Integer getWishlistId() { return wishlistId; }
    public void setWishlistId(Integer wishlistId) { this.wishlistId = wishlistId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ProductMaster getProduct() { return product; }
    public void setProduct(ProductMaster product) { this.product = product; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer wishlistId;
        private User user;
        private ProductMaster product;
        private LocalDateTime addedAt;

        public Builder wishlistId(Integer wishlistId) { this.wishlistId = wishlistId; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder product(ProductMaster product) { this.product = product; return this; }
        public Builder addedAt(LocalDateTime addedAt) { this.addedAt = addedAt; return this; }

        public Wishlist build() {
            return new Wishlist(wishlistId, user, product, addedAt);
        }
    }
}
