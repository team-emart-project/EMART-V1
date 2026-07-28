package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_id")
    private Integer prodId;

    @Column(name = "catmaster_id")
    private Integer catmasterId;

    @Column(name = "prod_name")
    private String prodName;

    @Column(name = "prod_short_desc")
    private String prodShortDesc;

    @Column(name = "prod_long_desc")
    private String prodLongDesc;

    @Column(name = "mrp_price")
    private BigDecimal mrpPrice;

    @Column(name = "cardholder_price")
    private BigDecimal cardholderPrice;

    @Column(name = "points_to_redeem")
    private Integer pointsToRedeem;

    @Column(name = "prod_image_path")
    private String prodImagePath;
    
    public Integer getProdId() {
        return prodId;
    }
}