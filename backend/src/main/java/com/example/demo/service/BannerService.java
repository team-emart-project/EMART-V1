package com.example.demo.service;

import com.example.demo.dto.response.BannerDTO;

import java.util.List;

public interface BannerService {

    // Public: only currently active & in-schedule banners, ordered for display
    List<BannerDTO> getActiveBanners();

    // Admin: every banner regardless of active/schedule status
    List<BannerDTO> getAllBanners();

    BannerDTO getBannerById(Long id);

    BannerDTO createBanner(BannerDTO bannerDTO);

    BannerDTO updateBanner(Long id, BannerDTO bannerDTO);

    void deleteBanner(Long id);
}
