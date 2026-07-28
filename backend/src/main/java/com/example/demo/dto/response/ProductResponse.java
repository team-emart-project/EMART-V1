package com.example.demo.dto.response;

import com.example.demo.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long productId;
    private Integer catmasterId;
    private String categoryName;
    private String productName;
    private String productShortDesc;
    private String productLongDesc;
    private String brand;
    private BigDecimal mrpPrice;
    private BigDecimal emcardPrice;
    private Integer redeemPoints;
    private String prodImagePath;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .catmasterId(product.getCategory() != null ? product.getCategory().getCatmasterId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .productName(product.getProductName())
                .productShortDesc(product.getProductShortDesc())
                .productLongDesc(product.getProductLongDesc())
                .brand(product.getBrand())
                .mrpPrice(product.getMrpPrice())
                .emcardPrice(product.getEmcardPrice())
                .redeemPoints(product.getRedeemPoints())
                .prodImagePath(product.getProdImagePath())
                .build();
    }
}
