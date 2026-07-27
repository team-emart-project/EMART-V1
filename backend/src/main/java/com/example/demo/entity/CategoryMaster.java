package com.example.demo.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catmaster_id")
    private Integer catmasterId;

    @Column(name = "cat_id")
    private String catId;

    @Column(name = "subcat_id")
    private String subcatId;

    @Column(name = "cat_name")
    private String catName;

    @Column(name = "cat_image_path")
    private String catImagePath;

    @Column(name = "flag")
    private Integer flag;
}
