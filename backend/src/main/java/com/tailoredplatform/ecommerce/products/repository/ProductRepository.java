package com.tailoredplatform.ecommerce.products.repository;

import com.tailoredplatform.ecommerce.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * JpaSpecificationExecutor backs the dynamic catalog filters (category, brand,
 * size, color, price range, rating, in-stock) built in ProductSpecifications —
 * see products.service.ProductQueryService.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    List<Product> findTop8ByIsBestSellerTrueAndStatusOrderByReviewCountDesc(Product.Status status);
    List<Product> findTop8ByIsNewArrivalTrueAndStatusOrderByCreatedAtDesc(Product.Status status);
}
