package com.example.demo.service.implementation;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.entity.CategoryMaster;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.CategoryMapper;
import com.example.demo.repository.CategoryMasterRepository;
import com.example.demo.service.interfaces.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Category browsing.
 *
 * The table stores the hierarchy with two CHAR(3) codes rather than a
 * parent_id foreign key (this comes from the original teacher-supplied design):
 *
 *   cat_id     = this row's own code            e.g. 'CAM'
 *   subcat_id  = the PARENT's code, or '^' for a root row
 *
 * So "children of X" means "every row whose subcat_id equals X.cat_id".
 */
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    /** The teacher's marker for "this row has no parent". */
    private static final String ROOT_MARKER = "^";

    /** Safety net so malformed data can never produce an infinite tree. */
    private static final int MAX_DEPTH = 10;

    private final CategoryMasterRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMasterRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryResponse> getCategoryTree() {

        // ONE read of the table, then the tree is assembled in memory.
        // Recursing with a query per level would hammer the database.
        List<CategoryMaster> all = categoryRepository.findAll();

        Map<String, List<CategoryMaster>> childrenByParentCode = all.stream()
                .filter(c -> c.getSubcatId() != null && !ROOT_MARKER.equals(c.getSubcatId()))
                .collect(Collectors.groupingBy(c -> c.getSubcatId().trim().toUpperCase()));

        List<CategoryMaster> roots = all.stream()
                .filter(this::isRoot)
                .sorted(Comparator.comparing(CategoryMaster::getCatName, Comparator.nullsLast(String::compareTo)))
                .toList();

        return roots.stream()
                .map(root -> buildNode(root, childrenByParentCode, new HashSet<>(), 0))
                .toList();
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findBySubcatIdOrderByCatNameAsc(ROOT_MARKER).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getSubCategories(Integer catmasterId) {
        CategoryMaster parent = loadCategory(catmasterId);
        return categoryRepository
                .findBySubcatIdIgnoreCaseOrderByCatNameAsc(parent.getCatId())
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategory(Integer catmasterId) {
        return categoryMapper.toResponse(loadCategory(catmasterId));
    }

    // ------------------------------------------------------------------

    private boolean isRoot(CategoryMaster category) {
        String parent = category.getSubcatId();
        return parent == null || parent.isBlank() || ROOT_MARKER.equals(parent.trim());
    }

    /**
     * Recursively attaches children.
     *
     * {@code visited} guards against a data cycle (a row whose subcat_id points
     * back up its own branch), which would otherwise recurse until the stack
     * overflows. depth is a second belt-and-braces limit.
     */
    private CategoryResponse buildNode(CategoryMaster category,
                                       Map<String, List<CategoryMaster>> childrenByParentCode,
                                       Set<Integer> visited,
                                       int depth) {

        CategoryResponse node = categoryMapper.toResponse(category);

        if (depth >= MAX_DEPTH || !visited.add(category.getCatmasterId())) {
            log.warn("Category tree stopped early at catmasterId={} (depth={}), check for a cycle in category_master",
                    category.getCatmasterId(), depth);
            node.setChildren(List.of());
            return node;
        }

        String code = category.getCatId() == null ? null : category.getCatId().trim().toUpperCase();
        List<CategoryMaster> children = code == null
                ? List.of()
                : childrenByParentCode.getOrDefault(code, List.of());

        List<CategoryResponse> childNodes = children.stream()
                .sorted(Comparator.comparing(CategoryMaster::getCatName, Comparator.nullsLast(String::compareTo)))
                .map(child -> buildNode(child, childrenByParentCode, new HashSet<>(visited), depth + 1))
                .toList();

        node.setChildren(childNodes);
        return node;
    }

    private CategoryMaster loadCategory(Integer catmasterId) {
        return categoryRepository.findById(catmasterId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "catmasterId", catmasterId));
    }
}
