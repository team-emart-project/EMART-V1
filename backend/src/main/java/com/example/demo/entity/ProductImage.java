package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * Maps the `product_image` table — the gallery shots for one product.
 *
 * product_master.prod_image_path stays as the single thumbnail used in
 * listings; this table holds the 4-6 images the detail-page slider needs.
 */
@Entity
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_image_id")
    private Integer prodImageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false)
    private ProductMaster product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    /** 0 first. Drives the order of the thumbnail strip. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    public ProductImage() {
    }

    public ProductImage(Integer prodImageId, ProductMaster product, String imageUrl,
                        String altText, Integer displayOrder, Boolean isPrimary) {
        this.prodImageId = prodImageId;
        this.product = product;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.displayOrder = displayOrder;
        this.isPrimary = isPrimary;
    }

    public Integer getProdImageId() { return prodImageId; }
    public void setProdImageId(Integer v) { this.prodImageId = v; }
    public ProductMaster getProduct() { return product; }
    public void setProduct(ProductMaster v) { this.product = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public String getAltText() { return altText; }
    public void setAltText(String v) { this.altText = v; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer v) { this.displayOrder = v; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean v) { this.isPrimary = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer prodImageId;
        private ProductMaster product;
        private String imageUrl;
        private String altText;
        private Integer displayOrder;
        private Boolean isPrimary;
        public Builder prodImageId(Integer v) { this.prodImageId = v; return this; }
        public Builder product(ProductMaster v) { this.product = v; return this; }
        public Builder imageUrl(String v) { this.imageUrl = v; return this; }
        public Builder altText(String v) { this.altText = v; return this; }
        public Builder displayOrder(Integer v) { this.displayOrder = v; return this; }
        public Builder isPrimary(Boolean v) { this.isPrimary = v; return this; }
        public ProductImage build() {
            return new ProductImage(prodImageId, product, imageUrl, altText, displayOrder, isPrimary);
        }
    }
}
