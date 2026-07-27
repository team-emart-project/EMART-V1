package com.example.demo.service.impl;

import com.example.demo.dto.response.BannerDTO;
import com.example.demo.entity.base.Banner;
import com.example.demo.exception.error.ResourceNotFoundException;
import com.example.demo.repository.BannerRepository;
import com.example.demo.service.BannerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository =null;

    @Override
    @Transactional(readOnly = true)
    public List<BannerDTO> getActiveBanners() {
        return bannerRepository.findActiveBanners(LocalDateTime.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerDTO> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BannerDTO getBannerById(Long id) {
        return toDTO(findBannerOrThrow(id));
    }

    @Override
    public BannerDTO createBanner(BannerDTO bannerDTO) {
        Banner banner = Banner.builder()
                .title(bannerDTO.getTitle())
                .imageUrl(bannerDTO.getImageUrl())
                .redirectUrl(bannerDTO.getRedirectUrl())
                .displayOrder(bannerDTO.getDisplayOrder() == null ? 0 : bannerDTO.getDisplayOrder())
                .active(bannerDTO.getActive() == null ? Boolean.TRUE : bannerDTO.getActive())
                .startDate(bannerDTO.getStartDate())
                .endDate(bannerDTO.getEndDate())
                .build();
        return toDTO(bannerRepository.save(banner));
    }

    @Override
    public BannerDTO updateBanner(Long id, BannerDTO bannerDTO) {
        Banner banner = findBannerOrThrow(id);
        banner.setTitle(bannerDTO.getTitle());
        banner.setImageUrl(bannerDTO.getImageUrl());
        banner.setRedirectUrl(bannerDTO.getRedirectUrl());
        if (bannerDTO.getDisplayOrder() != null) {
            banner.setDisplayOrder(bannerDTO.getDisplayOrder());
        }
        if (bannerDTO.getActive() != null) {
            banner.setActive(bannerDTO.getActive());
        }
        banner.setStartDate(bannerDTO.getStartDate());
        banner.setEndDate(bannerDTO.getEndDate());
        return toDTO(bannerRepository.save(banner));
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = findBannerOrThrow(id);
        bannerRepository.delete(banner);
    }

    private Banner findBannerOrThrow(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
    }

    private BannerDTO toDTO(Banner banner) {
        return BannerDTO.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .redirectUrl(banner.getRedirectUrl())
                .displayOrder(banner.getDisplayOrder())
                .active(banner.getActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .build();
    }
}
