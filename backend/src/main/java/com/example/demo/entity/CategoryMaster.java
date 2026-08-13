package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * Maps the `category_master` table (from your teacher's original design).
 *
 * Owned by Module 5 (Catalog); defined here because ProductMaster has a
 * foreign key to it and Module 6 loads products.
 */
@Entity
@Table(name = "category_master")
public class CategoryMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catmaster_id")
    private Integer catmasterId;

    @Column(name = "cat_id", nullable = false, length = 3)
    private String catId;

    /** '^' marks a root category in the seed data; otherwise it is a parent's cat_id. */
    @Column(name = "subcat_id", length = 3)
    private String subcatId;

    @Column(name = "cat_name", nullable = false, length = 255)
    private String catName;

    @Column(name = "cat_image_path", length = 255)
    private String catImagePath;

    @Column(name = "flag", nullable = false)
    private Boolean flag;

    public CategoryMaster() {
    }

    public CategoryMaster(Integer catmasterId, String catId, String subcatId, String catName,
                          String catImagePath, Boolean flag) {
        this.catmasterId = catmasterId;
        this.catId = catId;
        this.subcatId = subcatId;
        this.catName = catName;
        this.catImagePath = catImagePath;
        this.flag = flag;
    }

    public Integer getCatmasterId() {
        return catmasterId;
    }

    public void setCatmasterId(Integer catmasterId) {
        this.catmasterId = catmasterId;
    }

    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public String getSubcatId() {
        return subcatId;
    }

    public void setSubcatId(String subcatId) {
        this.subcatId = subcatId;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public String getCatImagePath() {
        return catImagePath;
    }

    public void setCatImagePath(String catImagePath) {
        this.catImagePath = catImagePath;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer catmasterId;
        private String catId;
        private String subcatId;
        private String catName;
        private String catImagePath;
        private Boolean flag;

        public Builder catmasterId(Integer catmasterId) {
            this.catmasterId = catmasterId;
            return this;
        }

        public Builder catId(String catId) {
            this.catId = catId;
            return this;
        }

        public Builder subcatId(String subcatId) {
            this.subcatId = subcatId;
            return this;
        }

        public Builder catName(String catName) {
            this.catName = catName;
            return this;
        }

        public Builder catImagePath(String catImagePath) {
            this.catImagePath = catImagePath;
            return this;
        }

        public Builder flag(Boolean flag) {
            this.flag = flag;
            return this;
        }

        public CategoryMaster build() {
            return new CategoryMaster(catmasterId, catId, subcatId, catName, catImagePath, flag);
        }
    }
}
