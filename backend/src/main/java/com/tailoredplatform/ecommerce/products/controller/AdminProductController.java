package com.tailoredplatform.ecommerce.products.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductCreateRequest;
import com.tailoredplatform.ecommerce.products.dto.ProductDetailResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductUpdateRequest;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.mapper.ProductMapper;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.products.service.ProductAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliberately separate from the customer-facing ProductController: an admin
 * needs to see DRAFT/INACTIVE/ARCHIVED products too, which
 * ProductSpecifications.fromFilter() intentionally always excludes for
 * shoppers. All endpoints here are @PreAuthorize'd, but backend authorization
 * is the actual boundary — Angular hiding the admin routes is not (per the
 * spec's own explicit requirement).
 *
 * Image "upload" here is URL-based (the admin pastes/hosts an image and
 * gives its URL) rather than a multipart file-upload pipeline — flagged as a
 * documented simplification; a real file upload would need object storage
 * (S3-compatible) wired in as its own concern.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Products", description = "Product/catalog management")
public class AdminProductController {

    private final ProductAdminService productAdminService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @GetMapping
    @Operation(summary = "List all products regardless of status, newest first")
    public PageResponse<ProductDetailResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = productRepository.findAll(pageable);
        return PageResponse.from(result.map(productMapper::toDetail));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new product (starts as DRAFT)")
    public ProductDetailResponse create(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productAdminService.create(request);
        return productMapper.toDetail(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product's core fields, including status")
    public ProductDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productAdminService.update(id, request);
        return productMapper.toDetail(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a product (soft delete — preserves order history integrity)")
    public void deactivate(@PathVariable Long id) {
        productAdminService.deactivate(id);
    }

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a product image by URL")
    public void addImage(
            @PathVariable Long id,
            @RequestParam String url,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "0") int displayOrder
    ) {
        productAdminService.addImage(id, url, altText, displayOrder);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Remove a product image")
    public void removeImage(@PathVariable Long id, @PathVariable Long imageId) {
        productAdminService.removeImage(id, imageId);
    }
}
