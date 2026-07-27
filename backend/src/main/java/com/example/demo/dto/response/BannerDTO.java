package com.example.demo.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerDTO {

    private Long id;

    @NotBlank(message = "Banner title is required")
    private String title;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private String redirectUrl;

    private Integer displayOrder;

    private Boolean active;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
