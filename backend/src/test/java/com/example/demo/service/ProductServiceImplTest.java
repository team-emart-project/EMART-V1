package com.example.demo.service;

import com.example.demo.dto.response.ProductResponse;
import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.entity.CategoryMaster;
import com.example.demo.entity.ConfigMaster;
import com.example.demo.entity.ProdDtlMaster;
import com.example.demo.entity.ProductMaster;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.repository.CategoryMasterRepository;
import com.example.demo.repository.ProdDtlMasterRepository;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.response.PageResponse;
import com.example.demo.service.implementation.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductMasterRepository productRepository;
    @Mock private CategoryMasterRepository categoryRepository;
    @Mock private ProdDtlMasterRepository prodDtlRepository;
    @Spy  private ProductMapper productMapper = new ProductMapper();

    @InjectMocks private ProductServiceImpl productService;

    private CategoryMaster canonDslr;
    private ProductMaster camera;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 12);

        canonDslr = CategoryMaster.builder()
                .catmasterId(7).catId("CNN").subcatId("DSL")
                .catName("Canon DSLR").flag(true).build();

        camera = ProductMaster.builder()
                .prodId(1).category(canonDslr)
                .prodName("Canon EOS 1500D DSLR Camera")
                .prodShortDesc("24.1MP DSLR")
                .prodLongDesc("Entry-level Canon DSLR camera.")
                .mrpPrice(new BigDecimal("32999.00"))
                .cardholderPrice(new BigDecimal("29999.00"))
                .build();
    }

    // ---------------- pricing / mapping ----------------

    @Test
    @DisplayName("summary exposes both prices and the cardholder saving")
    void summaryShowsBothPrices() {
        when(productRepository.search(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(camera), pageable, 1));

        PageResponse<ProductResponse> page =
                productService.searchProducts(null, null, null, pageable);

        ProductResponse dto = page.getContent().get(0);
        assertThat(dto.getMrpPrice()).isEqualByComparingTo("32999.00");
        assertThat(dto.getCardholderPrice()).isEqualByComparingTo("29999.00");
        assertThat(dto.getCardholderSaving()).isEqualByComparingTo("3000.00");
        assertThat(dto.getCategoryName()).isEqualTo("Canon DSLR");
        // list results stay lightweight
        assertThat(dto.getProdLongDesc()).isNull();
        assertThat(dto.getVariants()).isNull();
    }

    @Test
    @DisplayName("blank search term is treated as no filter, not as an empty match")
    void blankSearchBecomesNull() {
        when(productRepository.search(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService.searchProducts("   ", null, null, pageable);

        verify(productRepository).search(eq(null), eq(null), eq(null), any(Pageable.class));
    }

    // ---------------- price-range validation ----------------

    @Test
    @DisplayName("minPrice greater than maxPrice is rejected")
    void invertedPriceRangeRejected() {
        assertThatThrownBy(() -> productService.searchProducts(
                null, new BigDecimal("500"), new BigDecimal("100"), pageable))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("greater than");

        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("negative price is rejected")
    void negativePriceRejected() {
        assertThatThrownBy(() -> productService.searchProducts(
                null, new BigDecimal("-1"), null, pageable))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // ---------------- category branch ----------------

    @Test
    @DisplayName("category listing walks the whole branch so parent categories are not empty")
    void categoryListingIncludesDescendants() {
        CategoryMaster electronics = CategoryMaster.builder()
                .catmasterId(1).catId("ELE").subcatId("^").catName("Electronics").flag(false).build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(electronics));
        when(categoryRepository.findAll()).thenReturn(List.of(
                electronics,
                CategoryMaster.builder().catmasterId(4).catId("CAM").subcatId("ELE").catName("Cameras").flag(false).build(),
                CategoryMaster.builder().catmasterId(6).catId("DSL").subcatId("CAM").catName("DSLR").flag(false).build(),
                canonDslr
        ));
        when(productRepository.findByCategory_CatmasterIdIn(anyList(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(camera), pageable, 1));

        productService.getProductsByCategory(1, true, pageable);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).findByCategory_CatmasterIdIn(captor.capture(), any(Pageable.class));

        // Electronics + Cameras + DSLR + Canon DSLR
        assertThat(captor.getValue()).containsExactlyInAnyOrder(1, 4, 6, 7);
    }

    @Test
    @DisplayName("includeSubCategories=false queries only that one category")
    void directCategoryOnly() {
        when(categoryRepository.findById(7)).thenReturn(Optional.of(canonDslr));
        when(productRepository.findByCategory_CatmasterId(eq(7), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(camera), pageable, 1));

        productService.getProductsByCategory(7, false, pageable);

        verify(productRepository).findByCategory_CatmasterId(eq(7), any(Pageable.class));
        verify(productRepository, never()).findByCategory_CatmasterIdIn(anyList(), any(Pageable.class));
    }

    @Test
    @DisplayName("unknown category id gives a 404-style exception")
    void unknownCategoryFails() {
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductsByCategory(999, true, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    // ---------------- variants ----------------

    @Test
    @DisplayName("flat variant rows are grouped by attribute")
    void variantsAreGrouped() {
        ConfigMaster color = ConfigMaster.builder().configId(1).configName("Color").build();
        ConfigMaster storage = ConfigMaster.builder().configId(2).configName("Storage").build();

        when(productRepository.existsById(1)).thenReturn(true);
        when(prodDtlRepository.findByProductIdWithConfig(1)).thenReturn(List.of(
                variant(1, color, "Black"),
                variant(2, color, "Silver"),
                variant(3, storage, "64GB")
        ));

        List<ProductVariantResponse> variants = productService.getProductVariants(1);

        assertThat(variants).hasSize(2);   // 3 rows -> 2 attribute groups

        ProductVariantResponse colorGroup = variants.stream()
                .filter(v -> v.getConfigName().equals("Color")).findFirst().orElseThrow();
        assertThat(colorGroup.getValues()).extracting(ProductVariantResponse.VariantValue::getValue)
                .containsExactly("Black", "Silver");
    }

    @Test
    @DisplayName("a product with no variants returns an empty list, not an error")
    void noVariantsIsNotAnError() {
        when(productRepository.existsById(8)).thenReturn(true);
        when(prodDtlRepository.findByProductIdWithConfig(8)).thenReturn(List.of());

        assertThat(productService.getProductVariants(8)).isEmpty();
    }

    @Test
    @DisplayName("detail view includes long description and variants")
    void detailIsComplete() {
        when(productRepository.findByIdWithCategory(1)).thenReturn(Optional.of(camera));
        when(prodDtlRepository.findByProductIdWithConfig(1)).thenReturn(List.of());

        ProductResponse dto = productService.getProduct(1);

        assertThat(dto.getProdLongDesc()).isEqualTo("Entry-level Canon DSLR camera.");
        assertThat(dto.getVariants()).isNotNull();
    }

    @Test
    @DisplayName("unknown product id gives a 404-style exception")
    void unknownProductFails() {
        when(productRepository.findByIdWithCategory(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    private ProdDtlMaster variant(Integer id, ConfigMaster config, String value) {
        return ProdDtlMaster.builder()
                .prodDtlId(id).product(camera).config(config).configDtls(value).build();
    }
}
