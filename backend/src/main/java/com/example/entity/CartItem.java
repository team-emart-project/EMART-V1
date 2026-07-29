package com.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Maps the `cart_items` table — one row per distinct product in a cart.
 * Adding the same product twice increments {@code quantity} instead of
 * inserting a second row.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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


}
