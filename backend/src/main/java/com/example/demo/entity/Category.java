package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catmaster_id")
    private Integer catmasterId;

    // Business/display category code, e.g. "CAT-001"
    @Column(name = "cat_id", length = 11, nullable = false)
    private String catId;

    // Populated only for sub-categories; null/blank for top-level categories
    @Column(name = "subcat_id", length = 11)
    private String subCatId;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;

    @Column(name = "cat_image_path", length = 255)
    private String catImagePath;

    // Soft-delete / active flag. false ("N") = active, true ("Y") = disabled.
    @Builder.Default
    @Column(name = "flag", nullable = false)
    private Boolean flag = false;
}
