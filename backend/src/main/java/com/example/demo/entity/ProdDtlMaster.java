package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * Maps the `prod_dtl_master` table — one VALUE of one attribute for one product.
 *
 * Example rows for a camera:
 *   prod_id=4, config_id=1 (Color),   config_dtls='Black'
 *   prod_id=4, config_id=1 (Color),   config_dtls='Silver'
 *
 * The service groups these by config so the API returns
 * "Color: [Black, Silver]" rather than two unrelated rows.
 */
@Entity
@Table(name = "prod_dtl_master")
public class ProdDtlMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_dtl_id")
    private Integer prodDtlId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false)
    private ProductMaster product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private ConfigMaster config;

    @Column(name = "config_dtls", nullable = false, length = 255)
    private String configDtls;

    public ProdDtlMaster() {
    }

    public ProdDtlMaster(Integer prodDtlId, ProductMaster product, ConfigMaster config, String configDtls) {
        this.prodDtlId = prodDtlId;
        this.product = product;
        this.config = config;
        this.configDtls = configDtls;
    }

    public Integer getProdDtlId() {
        return prodDtlId;
    }

    public void setProdDtlId(Integer prodDtlId) {
        this.prodDtlId = prodDtlId;
    }

    public ProductMaster getProduct() {
        return product;
    }

    public void setProduct(ProductMaster product) {
        this.product = product;
    }

    public ConfigMaster getConfig() {
        return config;
    }

    public void setConfig(ConfigMaster config) {
        this.config = config;
    }

    public String getConfigDtls() {
        return configDtls;
    }

    public void setConfigDtls(String configDtls) {
        this.configDtls = configDtls;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer prodDtlId;
        private ProductMaster product;
        private ConfigMaster config;
        private String configDtls;

        public Builder prodDtlId(Integer prodDtlId) {
            this.prodDtlId = prodDtlId;
            return this;
        }

        public Builder product(ProductMaster product) {
            this.product = product;
            return this;
        }

        public Builder config(ConfigMaster config) {
            this.config = config;
            return this;
        }

        public Builder configDtls(String configDtls) {
            this.configDtls = configDtls;
            return this;
        }

        public ProdDtlMaster build() {
            return new ProdDtlMaster(prodDtlId, product, config, configDtls);
        }
    }
}
