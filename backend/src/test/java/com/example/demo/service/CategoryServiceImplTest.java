package com.example.demo.service;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.entity.CategoryMaster;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.CategoryMapper;
import com.example.demo.repository.CategoryMasterRepository;
import com.example.demo.service.implementation.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests the tree assembly, which is the only real logic in this service —
 * the hierarchy is encoded as CHAR(3) codes rather than a parent_id, so this
 * is where mistakes would hide.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryMasterRepository categoryRepository;
    @Spy  private CategoryMapper categoryMapper = new CategoryMapper();

    @InjectMocks private CategoryServiceImpl categoryService;

    private List<CategoryMaster> seedLikeData;

    @BeforeEach
    void setUp() {
        // Mirrors emart_seed_data.sql:
        // Electronics -> Cameras -> DSLR Cameras -> Canon DSLR
        seedLikeData = List.of(
                cat(1, "ELE", "^",   "Electronics"),
                cat(2, "HAP", "^",   "Home Appliances"),
                cat(4, "CAM", "ELE", "Cameras"),
                cat(5, "TVS", "ELE", "Televisions"),
                cat(6, "DSL", "CAM", "DSLR Cameras"),
                cat(7, "CNN", "DSL", "Canon DSLR")
        );
    }

    @Test
    @DisplayName("tree nests four levels deep from the flat cat_id/subcat_id codes")
    void buildsDeepTree() {
        when(categoryRepository.findAll()).thenReturn(seedLikeData);

        List<CategoryResponse> tree = categoryService.getCategoryTree();

        // two roots, alphabetical: Electronics, Home Appliances
        assertThat(tree).hasSize(2);
        CategoryResponse electronics = tree.get(0);
        assertThat(electronics.getCatName()).isEqualTo("Electronics");

        // Electronics -> Cameras, Televisions
        assertThat(electronics.getChildren()).hasSize(2);
        CategoryResponse cameras = electronics.getChildren().get(0);
        assertThat(cameras.getCatName()).isEqualTo("Cameras");

        // Cameras -> DSLR Cameras -> Canon DSLR
        assertThat(cameras.getChildren()).hasSize(1);
        CategoryResponse dslr = cameras.getChildren().get(0);
        assertThat(dslr.getCatName()).isEqualTo("DSLR Cameras");
        assertThat(dslr.getChildren()).hasSize(1);
        assertThat(dslr.getChildren().get(0).getCatName()).isEqualTo("Canon DSLR");
    }

    @Test
    @DisplayName("only rows marked '^' are treated as roots")
    void onlyCaretRowsAreRoots() {
        when(categoryRepository.findAll()).thenReturn(seedLikeData);

        List<String> rootNames = categoryService.getCategoryTree().stream()
                .map(CategoryResponse::getCatName).toList();

        assertThat(rootNames).containsExactly("Electronics", "Home Appliances");
        assertThat(rootNames).doesNotContain("Cameras", "DSLR Cameras");
    }

    @Test
    @DisplayName("leaf categories come back with an empty children list, not null")
    void leavesHaveEmptyChildren() {
        when(categoryRepository.findAll()).thenReturn(List.of(cat(1, "ELE", "^", "Electronics")));

        CategoryResponse root = categoryService.getCategoryTree().get(0);

        assertThat(root.getChildren()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("a cycle in the data does not cause infinite recursion")
    void cyclicDataIsSurvivable() {
        // A points to B, B points back to A — corrupt data, must not hang.
        when(categoryRepository.findAll()).thenReturn(List.of(
                cat(1, "AAA", "^",   "Root"),
                cat(2, "BBB", "AAA", "Child"),
                cat(3, "AAA", "BBB", "Cycles back")
        ));

        List<CategoryResponse> tree = categoryService.getCategoryTree();

        assertThat(tree).isNotEmpty();  // returned rather than StackOverflowError
    }

    @Test
    @DisplayName("parent code matching ignores case and padding")
    void parentMatchingIsLenient() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                cat(1, "ELE", "^",    "Electronics"),
                cat(2, "CAM", " ele ", "Cameras")   // CHAR(3) padding + wrong case
        ));

        CategoryResponse root = categoryService.getCategoryTree().get(0);

        assertThat(root.getChildren()).hasSize(1);
        assertThat(root.getChildren().get(0).getCatName()).isEqualTo("Cameras");
    }

    @Test
    @DisplayName("unknown category id gives a 404-style exception")
    void unknownCategoryFails() {
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    private CategoryMaster cat(Integer id, String catId, String subcatId, String name) {
        return CategoryMaster.builder()
                .catmasterId(id).catId(catId).subcatId(subcatId)
                .catName(name).flag(false).build();
    }
}
