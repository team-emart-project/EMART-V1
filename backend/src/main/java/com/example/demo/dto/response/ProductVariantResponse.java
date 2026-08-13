package com.example.demo.dto.response;

import java.util.List;

/**
 * Variants GROUPED by attribute, e.g.
 *   { "configId": 1, "configName": "Color", "values": [ {...Black}, {...Silver} ] }
 *
 * The database stores one flat row per value in prod_dtl_master; grouping is
 * done in the service so the frontend can render one dropdown per attribute.
 */
public class ProductVariantResponse {

    private Integer configId;
    private String configName;
    private List<VariantValue> values;

    public ProductVariantResponse() {
    }

    public ProductVariantResponse(Integer configId, String configName, List<VariantValue> values) {
        this.configId = configId;
        this.configName = configName;
        this.values = values;
    }

    public Integer getConfigId() {
        return configId;
    }

    public void setConfigId(Integer configId) {
        this.configId = configId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public List<VariantValue> getValues() {
        return values;
    }

    public void setValues(List<VariantValue> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer configId;
        private String configName;
        private List<VariantValue> values;

        public Builder configId(Integer configId) {
            this.configId = configId;
            return this;
        }

        public Builder configName(String configName) {
            this.configName = configName;
            return this;
        }

        public Builder values(List<VariantValue> values) {
            this.values = values;
            return this;
        }

        public ProductVariantResponse build() {
            return new ProductVariantResponse(configId, configName, values);
        }
    }

    public static class VariantValue {

        private Integer prodDtlId;
        private String value;

        public VariantValue() {
        }

        public VariantValue(Integer prodDtlId, String value) {
            this.prodDtlId = prodDtlId;
            this.value = value;
        }

        public Integer getProdDtlId() {
            return prodDtlId;
        }

        public void setProdDtlId(Integer prodDtlId) {
            this.prodDtlId = prodDtlId;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private Integer prodDtlId;
            private String value;

            public Builder prodDtlId(Integer prodDtlId) {
                this.prodDtlId = prodDtlId;
                return this;
            }

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public VariantValue build() {
                return new VariantValue(prodDtlId, value);
            }
        }
    }
}
