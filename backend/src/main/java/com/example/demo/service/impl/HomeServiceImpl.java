package com.example.demo.service.impl;

import com.example.demo.dto.response.BannerDTO;
import com.example.demo.dto.response.CategoryDTO;
import com.example.demo.dto.response.HomePageResponse;
import com.example.demo.dto.response.ProductDTO;
import com.example.demo.entity.base.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.BannerService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.HomeService;
//import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

        private final BannerService bannerService;
        private final CategoryService categoryService;
        private final ProductRepository productRepository;

        public HomeServiceImpl(BannerService bannerService, CategoryService categoryService,
                        ProductRepository productRepository) {
                this.bannerService = bannerService;
                this.categoryService = categoryService;
                this.productRepository = productRepository;
        }

        @Override
        public HomePageResponse getHomePageData() {
                List<BannerDTO> banners = bannerService.getActiveBanners();
                List<CategoryDTO> categories = categoryService.getAllCategories();

                List<ProductDTO> featuredProducts = productRepository
                                .findTop10ByActiveTrueAndFeaturedTrueOrderByIdDesc()
                                .stream().map(this::toDTO).collect(Collectors.toList());

                List<ProductDTO> newArrivals = productRepository
                                .findTop10ByActiveTrueOrderByCreatedAtDesc()
                                .stream().map(this::toDTO).collect(Collectors.toList());

                List<ProductDTO> bestSellers = productRepository
                                .findTop10ByActiveTrueOrderBySalesCountDesc()
                                .stream().map(this::toDTO).collect(Collectors.toList());

                return HomePageResponse.builder()
                                .banners(banners)
                                .categories(categories)
                                .featuredProducts(featuredProducts)
                                .newArrivals(newArrivals)
                                .bestSellers(bestSellers)
                                .build();
        }

        private ProductDTO toDTO(Product product) {
                return ProductDTO.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .description(product.getDescription())
                                .price(product.getPrice())
                                .stockQuantity(product.getStockQuantity())
                                .imageUrl(product.getImageUrl())
                                .active(product.getActive())
                                .featured(product.getFeatured())
                                .salesCount(product.getSalesCount())
                                .categoryId(product.getCategory().getId())
                                .categoryName(product.getCategory().getName())
                                .createdAt(product.getCreatedAt())
                                .updatedAt(product.getUpdatedAt())
                                .build();
        }
}
