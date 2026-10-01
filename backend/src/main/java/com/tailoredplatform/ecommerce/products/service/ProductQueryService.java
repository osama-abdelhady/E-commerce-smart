package com.tailoredplatform.ecommerce.products.service;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.products.dto.ProductDetailResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductFilter;
import com.tailoredplatform.ecommerce.products.dto.ProductSummaryResponse;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.mapper.ProductMapper;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.products.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private static final int RELATED_PRODUCTS_LIMIT = 8;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public PageResponse<ProductSummaryResponse> search(ProductFilter filter) {
        // Sort.unsorted(): ordering is applied inside ProductSpecifications
        // itself (it needs the COALESCE(discount_price, price) expression,
        // which a plain Pageable Sort property path can't express).
        var pageable = PageRequest.of(filter.page(), filter.pageSize(), Sort.unsorted());
        Page<Product> page = productRepository.findAll(ProductSpecifications.fromFilter(filter), pageable);
        // toSummaryList batches the inventory lookup for the whole page in
        // one query (Phase 9 fix) instead of page.map(productMapper::toSummary),
        // which issued one inventory query per product/variant.
        List<ProductSummaryResponse> mapped = productMapper.toSummaryList(page.getContent());
        return new PageResponse<>(mapped, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", slug));
        return productMapper.toDetail(product);
    }

    public ProductDetailResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
        return productMapper.toDetail(product);
    }

    /** Related = same category, excluding the product itself, active only, capped at 8. */
    public List<ProductSummaryResponse> getRelated(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        var filter = new ProductFilter(
                product.getCategory().getSlug(), null, null, null,
                null, null, null, null, null,
                com.tailoredplatform.ecommerce.products.dto.ProductSortOption.POPULARITY,
                0, RELATED_PRODUCTS_LIMIT + 1
        );
        var pageable = PageRequest.of(0, RELATED_PRODUCTS_LIMIT + 1, Sort.unsorted());

        List<Product> related = productRepository.findAll(ProductSpecifications.fromFilter(filter), pageable)
                .stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(RELATED_PRODUCTS_LIMIT)
                .toList();
        return productMapper.toSummaryList(related);
    }

    public List<ProductSummaryResponse> getBestSellers(int limit) {
        List<Product> products = productRepository
                .findTop8ByIsBestSellerTrueAndStatusOrderByReviewCountDesc(Product.Status.ACTIVE)
                .stream()
                .limit(limit)
                .toList();
        return productMapper.toSummaryList(products);
    }

    public List<ProductSummaryResponse> getNewArrivals(int limit) {
        List<Product> products = productRepository
                .findTop8ByIsNewArrivalTrueAndStatusOrderByCreatedAtDesc(Product.Status.ACTIVE)
                .stream()
                .limit(limit)
                .toList();
        return productMapper.toSummaryList(products);
    }
}
