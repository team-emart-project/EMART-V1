package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A category. When returned from the tree endpoint, {@code children} is
 * populated recursively; in flat listings it is left null and omitted from JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private Integer catmasterId;
    private String catId;
    private String subcatId;
    private String catName;
    private String catImagePath;

    /**
     * The teacher's `flag` column: true means "this row points straight at a
     * product page" rather than at another level of categories.
     */
    private Boolean flag;

    /** Direct sub-categories. Null (and omitted) in flat responses. */
    private List<CategoryResponse> children;

    public CategoryResponse() {
    }

    public CategoryResponse(Integer catmasterId, String catId, String subcatId, String catName,
                            String catImagePath, Boolean flag, List<CategoryResponse> children) {
        this.catmasterId = catmasterId;
        this.catId = catId;
        this.subcatId = subcatId;
        this.catName = catName;
        this.catImagePath = catImagePath;
        this.flag = flag;
        this.children = children;
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

    public List<CategoryResponse> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryResponse> children) {
        this.children = children;
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
        private List<CategoryResponse> children;

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

        public Builder children(List<CategoryResponse> children) {
            this.children = children;
            return this;
        }

        public CategoryResponse build() {
            return new CategoryResponse(catmasterId, catId, subcatId, catName, catImagePath, flag, children);
        }
    }
}
