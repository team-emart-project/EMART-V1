package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "config_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Integer configId;

    @Column(name = "config_name")
    private String configName;
}