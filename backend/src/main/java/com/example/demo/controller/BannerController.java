package com.example.demo.controller;

import com.example.demo.dto.response.BannerDTO;
import com.example.demo.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    // GET /api/banners – public: active, in-schedule banners for the homepage slider
    @GetMapping
    public ResponseEntity<List<BannerDTO>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    // GET /api/banners/all – admin: every banner regardless of status
    @GetMapping("/all")
    public ResponseEntity<List<BannerDTO>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerDTO> getBannerById(@PathVariable Long id) {
        return ResponseEntity.ok(bannerService.getBannerById(id));
    }

    // POST /api/banners – admin: add a banner
    @PostMapping
    public ResponseEntity<BannerDTO> createBanner(@Valid @RequestBody BannerDTO bannerDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.createBanner(bannerDTO));
    }

    // PUT /api/banners/{id} – admin: update a banner
    @PutMapping("/{id}")
    public ResponseEntity<BannerDTO> updateBanner(@PathVariable Long id,
                                                   @Valid @RequestBody BannerDTO bannerDTO) {
        return ResponseEntity.ok(bannerService.updateBanner(id, bannerDTO));
    }

    // DELETE /api/banners/{id} – admin: remove a banner
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
