package com.tailoredplatform.ecommerce.categories.service;

import com.tailoredplatform.ecommerce.categories.dto.BrandResponse;
import com.tailoredplatform.ecommerce.categories.dto.CategoryResponse;
import com.tailoredplatform.ecommerce.categories.entity.Category;
import com.tailoredplatform.ecommerce.categories.mapper.CategoryMapper;
import com.tailoredplatform.ecommerce.categories.repository.BrandRepository;
import com.tailoredplatform.ecommerce.categories.repository.CategoryRepository;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryMapper categoryMapper;

    /** Top-level categories with their direct children nested — powers the navbar mega-menu. */
    public List<CategoryResponse> getCategoryTree() {
        List<Category> all = categoryRepository.findByIsActiveTrueOrderByDisplayOrder();

        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(parent -> categoryMapper.toResponse(parent, childrenOf(parent, all)))
                .toList();
    }

    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", slug));
        List<Category> all = categoryRepository.findByIsActiveTrueOrderByDisplayOrder();
        return categoryMapper.toResponse(category, childrenOf(category, all));
    }

    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .filter(b -> b.isActive())
                .map(categoryMapper::toResponse)
                .toList();
    }

    private List<CategoryResponse> childrenOf(Category parent, List<Category> all) {
        return all.stream()
                .filter(c -> c.getParent() != null && c.getParent().getId().equals(parent.getId()))
                .map(child -> categoryMapper.toResponse(child, List.of()))
                .collect(Collectors.toList());
    }
}
