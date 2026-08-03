package com.example.demo.entity.base;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    // Optional link target, e.g. a category or product page
    @Column(name = "redirect_url")
    private String redirectUrl;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // Optional scheduling window; null means "always shown while active"
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}
