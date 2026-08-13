package com.example.demo.dto.response;

/** One gallery image. */
public class ProductImageResponse {

    private Integer prodImageId;
    private String imageUrl;
    private String altText;
    private Integer displayOrder;
    private Boolean isPrimary;

    public ProductImageResponse() {
    }

    public ProductImageResponse(Integer prodImageId, String imageUrl, String altText,
                                Integer displayOrder, Boolean isPrimary) {
        this.prodImageId = prodImageId;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.displayOrder = displayOrder;
        this.isPrimary = isPrimary;
    }

    public Integer getProdImageId() { return prodImageId; }
    public void setProdImageId(Integer v) { this.prodImageId = v; }
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
        private String imageUrl;
        private String altText;
        private Integer displayOrder;
        private Boolean isPrimary;
        public Builder prodImageId(Integer v) { this.prodImageId = v; return this; }
        public Builder imageUrl(String v) { this.imageUrl = v; return this; }
        public Builder altText(String v) { this.altText = v; return this; }
        public Builder displayOrder(Integer v) { this.displayOrder = v; return this; }
        public Builder isPrimary(Boolean v) { this.isPrimary = v; return this; }
        public ProductImageResponse build() {
            return new ProductImageResponse(prodImageId, imageUrl, altText, displayOrder, isPrimary);
        }
    }
}
