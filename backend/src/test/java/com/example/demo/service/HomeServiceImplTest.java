package com.example.demo.service;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.entity.CategoryMaster;
import com.example.demo.entity.ProductMaster;
import com.example.demo.mapper.CategoryMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.repository.CategoryMasterRepository;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.service.implementation.HomeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock private CategoryMasterRepository categoryRepository;
    @Mock private ProductMasterRepository productRepository;
    @Spy  private CategoryMapper categoryMapper = new CategoryMapper();
    @Spy  private ProductMapper productMapper = new ProductMapper();

    @InjectMocks private HomeServiceImpl homeService;

    @Test
    @DisplayName("featured categories come from the flag column")
    void featuredCategoriesUseFlag() {
        when(categoryRepository.findByFlagTrueOrderByCatNameAsc()).thenReturn(List.of(
                CategoryMaster.builder().catmasterId(8).catId("OP5").subcatId("^")
                        .catName("One Plus 55\" LED TV").flag(true).build(),
                CategoryMaster.builder().catmasterId(9).catId("CNR").subcatId("^")
                        .catName("Canon EOS R10").flag(true).build()));

        List<CategoryResponse> featured = homeService.getFeaturedCategories();

        assertThat(featured).hasSize(2);
        assertThat(featured).allMatch(CategoryResponse::getFlag);
        verify(categoryRepository).findByFlagTrueOrderByCatNameAsc();
    }

    @Test
    @DisplayName("new arrivals request exactly the number asked for")
    void newArrivalsRespectsLimit() {
        when(productRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(product()));

        homeService.getNewArrivals(5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("an absurd limit is clamped instead of pulling the whole catalog")
    void hugeLimitIsClamped() {
        when(productRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

        homeService.getNewArrivals(100_000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("a zero or negative limit is raised to 1 rather than throwing")
    void nonPositiveLimitIsClamped() {
        when(productRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

        homeService.getNewArrivals(0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("new arrivals expose both prices and the cardholder saving")
    void newArrivalsShowPricing() {
        when(productRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(product()));

        List<ProductResponse> arrivals = homeService.getNewArrivals(8);

        assertThat(arrivals).hasSize(1);
        assertThat(arrivals.get(0).getCardholderSaving()).isEqualByComparingTo("3000.00");
    }

    private ProductMaster product() {
        return ProductMaster.builder()
                .prodId(1)
                .category(CategoryMaster.builder().catmasterId(7).catName("Canon DSLR").build())
                .prodName("Canon EOS 1500D")
                .mrpPrice(new BigDecimal("32999.00"))
                .cardholderPrice(new BigDecimal("29999.00"))
                .build();
    }
}
