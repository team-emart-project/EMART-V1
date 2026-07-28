package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catmaster_id", referencedColumnName = "catmaster_id", nullable = false)
    private Category category;

    @Column(name = "product_name", length = 150, nullable = false)
    private String productName;

    @Column(name = "product_shortdesc", length = 255)
    private String productShortDesc;

    @Column(name = "product_longdesc", columnDefinition = "TEXT")
    private String productLongDesc;

    // NOTE: not present in the BRD column list you shared, but the BRD text (section on
    // category/item-list filtering) explicitly requires filtering "by brand and/or price",
    // so this column is added to make GET /api/products/filter?brand= possible.
    // Drop it if brand actually lives in a separate lookup table.
    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "mrp_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal mrpPrice;

    @Column(name = "emcard_price", precision = 10, scale = 2)
    private BigDecimal emcardPrice;

    @Column(name = "redeem_points")
    private Integer redeemPoints;

    @Column(name = "prod_image_path", length = 255)
    private String prodImagePath;
}
