package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * Maps the `config_master` table.
 *
 * This is the ATTRIBUTE TYPE of a product variant — "Color", "Storage", "Size".
 * The actual values ("Black", "64GB") live in prod_dtl_master.
 */
@Entity
@Table(name = "config_master")
public class ConfigMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Integer configId;

    @Column(name = "config_name", nullable = false, length = 100)
    private String configName;

    public ConfigMaster() {
    }

    public ConfigMaster(Integer configId, String configName) {
        this.configId = configId;
        this.configName = configName;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer configId;
        private String configName;

        public Builder configId(Integer configId) {
            this.configId = configId;
            return this;
        }

        public Builder configName(String configName) {
            this.configName = configName;
            return this;
        }

        public ConfigMaster build() {
            return new ConfigMaster(configId, configName);
        }
    }
}
