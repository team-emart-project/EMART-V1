package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prod_dtl_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdDtlMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_dtl_id")
    private Integer prodDtlId;

    @Column(name = "prod_id")
    private Integer prodId;

    @Column(name = "config_id")
    private Integer configId;

    @Column(name = "config_dtls")
    private String configDtls;
}