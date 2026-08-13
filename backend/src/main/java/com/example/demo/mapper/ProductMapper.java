package com.example.demo.mapper;

import com.example.demo.dto.response.ProductImageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.entity.ProdDtlMaster;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.ProductMaster;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductMapper {

    /**
     * PRICE VISIBILITY RULE
     * ---------------------
     * mrp_price is ALWAYS shown - to everyone, signed in or not.
     *
     * All three e-MART card offers (member price, points-only price, combo
     * price) are shown ONLY to a user with an ACTIVE e-MART card. For everyone
     * else they are set to null, and @JsonInclude(NON_NULL) on ProductResponse
     * drops them from the JSON entirely - so a non-cardholder cannot read the
     * member pricing by opening devtools.
     *
     * Null carries a second meaning: "this product does not offer that
     * option". Both reasons produce the same UI outcome (no checkbox), so the
     * product card needs only one null check per option.
     */
    public ProductResponse toSummary(ProductMaster product, boolean isCardholder) {
        ProductResponse response = toSummary(product);
        if (!isCardholder) {
            hideMemberOffers(response);
        }
        return response;
    }

    public ProductResponse toDetail(ProductMaster product,
                                    List<ProductVariantResponse> variants,
                                    List<ProductImage> images,
                                    boolean isCardholder) {
        ProductResponse response = toDetail(product, variants);
        response.setImages(toImageResponses(images));
        if (!isCardholder) {
            hideMemberOffers(response);
        }
        return response;
    }

    /** Internal form that always includes both prices. */
    public ProductResponse toSummary(ProductMaster product) {
        return ProductResponse.builder()
                .prodId(product.getProdId())
                .prodName(product.getProdName())
                .prodShortDesc(product.getProdShortDesc())
                .mrpPrice(product.getMrpPrice())
                .cardholderPrice(product.getCardholderPrice())
                .cardholderSaving(saving(product))
                .pointsPrice(product.getPointsPrice())
                .hybridCashPrice(product.getHybridCashPrice())
                .hybridPoints(product.getHybridPoints())
                .brand(product.getBrand())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .rating(product.getRating())
                .ratingCount(product.getRatingCount())
                .discountPercentage(product.getDiscountPercentage())
                .prodImagePath(product.getProdImagePath())
                .catmasterId(product.getCategory() != null ? product.getCategory().getCatmasterId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCatName() : null)
                .build();
    }

    /** Full form for the detail endpoint. */
    public ProductResponse toDetail(ProductMaster product, List<ProductVariantResponse> variants) {
        ProductResponse response = toSummary(product);
        response.setProdLongDesc(product.getProdLongDesc());
        response.setVariants(variants);
        return response;
    }

    /**
     * Turns flat prod_dtl_master rows into one entry per attribute.
     * LinkedHashMap preserves the ordering the query already applied.
     */
    public List<ProductVariantResponse> groupVariants(List<ProdDtlMaster> details) {

        Map<Integer, ProductVariantResponse> grouped = new LinkedHashMap<>();

        for (ProdDtlMaster detail : details) {
            Integer configId = detail.getConfig().getConfigId();

            ProductVariantResponse group = grouped.computeIfAbsent(configId, id ->
                    ProductVariantResponse.builder()
                            .configId(id)
                            .configName(detail.getConfig().getConfigName())
                            .values(new java.util.ArrayList<>())
                            .build());

            group.getValues().add(ProductVariantResponse.VariantValue.builder()
                    .prodDtlId(detail.getProdDtlId())
                    .value(detail.getConfigDtls())
                    .build());
        }

        return List.copyOf(grouped.values());
    }

    public ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .prodImageId(image.getProdImageId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .displayOrder(image.getDisplayOrder())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    public List<ProductImageResponse> toImageResponses(List<ProductImage> images) {
        if (images == null || images.isEmpty()) return List.of();
        return images.stream().map(this::toImageResponse).toList();
    }

    /**
     * Strips every member-only figure in one place. One method, so adding a
     * fourth offer later cannot leak by being forgotten in one of two branches.
     */
    private void hideMemberOffers(ProductResponse response) {
        response.setCardholderPrice(null);
        response.setCardholderSaving(null);
        response.setPointsPrice(null);
        response.setHybridCashPrice(null);
        response.setHybridPoints(null);
    }

    /** Null rather than ZERO when there is no member price, so NON_NULL omits it. */
    private BigDecimal saving(ProductMaster product) {
        if (product.getMrpPrice() == null || product.getCardholderPrice() == null) {
            return null;
        }
        return product.getMrpPrice().subtract(product.getCardholderPrice());
    }
}
