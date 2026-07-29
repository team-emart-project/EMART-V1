package com.example.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Maps the `category_master` table (from your teacher's original design).
 *
 * Owned by Module 5 (Catalog); defined here because ProductMaster has a
 * foreign key to it and Module 6 loads products.
 */
@Entity
@Table(name = "category_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
