package com.tailoredplatform.ecommerce.wishlist.controller;

import com.tailoredplatform.ecommerce.security.UserPrincipal;
import com.tailoredplatform.ecommerce.wishlist.dto.WishlistResponse;
import com.tailoredplatform.ecommerce.wishlist.mapper.WishlistMapper;
import com.tailoredplatform.ecommerce.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Authenticated wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final WishlistMapper wishlistMapper;

    @GetMapping
    @Operation(summary = "Get the current user's wishlist")
    public WishlistResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return wishlistMapper.toResponse(wishlistService.getOrCreate(principal.getId()));
    }

    @PostMapping("/items/{productId}")
    @Operation(summary = "Add a product to the wishlist")
    public WishlistResponse add(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        return wishlistMapper.toResponse(wishlistService.addItem(principal.getId(), productId));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the wishlist")
    public WishlistResponse remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        return wishlistMapper.toResponse(wishlistService.removeItem(principal.getId(), productId));
    }

    @PostMapping("/items/{productId}/move-to-cart")
    @Operation(summary = "Move a wishlisted product into the cart")
    public WishlistResponse moveToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        return wishlistMapper.toResponse(wishlistService.moveToCart(principal.getId(), productId, variantId, quantity));
    }
}
