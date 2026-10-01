package com.tailoredplatform.ecommerce.cart.controller;

import com.tailoredplatform.ecommerce.cart.dto.AddCartItemRequest;
import com.tailoredplatform.ecommerce.cart.dto.CartResponse;
import com.tailoredplatform.ecommerce.cart.dto.UpdateCartItemRequest;
import com.tailoredplatform.ecommerce.cart.mapper.CartMapper;
import com.tailoredplatform.ecommerce.cart.service.CartService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Authenticated shopping cart")
public class CartController {

    private final CartService cartService;
    private final CartMapper cartMapper;

    @GetMapping
    @Operation(summary = "Get the current user's cart")
    public CartResponse getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return cartMapper.toResponse(cartService.getOrCreateCart(principal.getId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart (merges quantity if the variant is already present)")
    public CartResponse addItem(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody AddCartItemRequest request) {
        return cartMapper.toResponse(cartService.addItem(principal.getId(), request.productVariantId(), request.quantity()));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update the quantity of a cart line item")
    public CartResponse updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartMapper.toResponse(cartService.updateItemQuantity(principal.getId(), itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a line item from the cart")
    public CartResponse removeItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long itemId) {
        return cartMapper.toResponse(cartService.removeItem(principal.getId(), itemId));
    }

    @DeleteMapping
    @Operation(summary = "Clear the entire cart")
    public void clear(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clear(principal.getId());
    }
}
