package com.example.entity;

import jakarta.persistence.*;
import lombok.*;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
