package com.tailoredplatform.ecommerce.categories.repository;

import com.tailoredplatform.ecommerce.categories.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByIsActiveTrueOrderByDisplayOrder();
    List<Category> findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrder();
}
