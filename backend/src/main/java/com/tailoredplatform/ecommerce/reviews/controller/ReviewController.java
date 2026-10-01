package com.tailoredplatform.ecommerce.reviews.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.reviews.dto.ReviewRequest;
import com.tailoredplatform.ecommerce.reviews.dto.ReviewResponse;
import com.tailoredplatform.ecommerce.reviews.mapper.ReviewMapper;
import com.tailoredplatform.ecommerce.reviews.service.ReviewService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews — public reads, purchase-gated writes")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;

    @GetMapping("/api/v1/products/{productId}/reviews")
    @Operation(summary = "List published reviews for a product")
    public PageResponse<ReviewResponse> listForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var reviews = reviewService.listForProduct(productId, pageable);
        return PageResponse.from(reviews.map(reviewMapper::toResponse));
    }

    @PostMapping("/api/v1/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Leave a review for a purchased order item")
    public ReviewResponse create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ReviewRequest request) {
        return reviewMapper.toResponse(reviewService.create(principal.getUser(), request));
    }

    @PutMapping("/api/v1/reviews/{id}")
    @Operation(summary = "Update your own review")
    public ReviewResponse update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return reviewMapper.toResponse(reviewService.update(principal.getUser(), id, request));
    }

    @DeleteMapping("/api/v1/reviews/{id}")
    @Operation(summary = "Delete your own review")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        reviewService.delete(principal.getUser(), id);
    }
}
