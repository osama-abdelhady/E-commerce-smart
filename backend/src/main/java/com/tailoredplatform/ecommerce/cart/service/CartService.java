package com.tailoredplatform.ecommerce.cart.service;

import com.tailoredplatform.ecommerce.cart.entity.Cart;
import com.tailoredplatform.ecommerce.cart.entity.CartItem;
import com.tailoredplatform.ecommerce.cart.repository.CartItemRepository;
import com.tailoredplatform.ecommerce.cart.repository.CartRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import com.tailoredplatform.ecommerce.products.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The cart is a convenience/display layer only. unitPriceSnapshot here is
 * NEVER what gets charged — OrderService re-derives every line item's price
 * from ProductVariant.effectivePrice() at checkout time, independent of
 * whatever this cart currently shows. See CartItem's javadoc.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private static final int MAX_QUANTITY_PER_LINE = 20;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            // Defensive fallback — AuthService.register() already provisions
            // one, but this keeps CartService safe to call independently
            // (e.g. from tests, or for accounts created before this existed).
            throw new ResourceNotFoundException("Cart not found for user " + userId + ". This should not happen for a registered account.");
        });
    }

    public Cart addItem(Long userId, Long productVariantId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", productVariantId));

        if (!variant.isActive()) {
            throw new BusinessRuleViolationException("This product variant is no longer available.");
        }

        var existing = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), productVariantId);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQuantity = Math.min(item.getQuantity() + quantity, MAX_QUANTITY_PER_LINE);
            item.setQuantity(newQuantity);
            item.setUnitPriceSnapshot(variant.effectivePrice());
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductVariant(variant);
            item.setQuantity(Math.min(quantity, MAX_QUANTITY_PER_LINE));
            item.setUnitPriceSnapshot(variant.effectivePrice());
            item.setAddedAt(Instant.now());
            cartItemRepository.save(item);
            cart.getItems().add(item);
        }

        return cartRepository.findByUserId(userId).orElseThrow();
    }

    public Cart updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = requireOwnedItem(cart, cartItemId);
        item.setQuantity(Math.min(quantity, MAX_QUANTITY_PER_LINE));
        item.setUnitPriceSnapshot(item.getProductVariant().effectivePrice());
        cartItemRepository.save(item);
        return cartRepository.findByUserId(userId).orElseThrow();
    }

    public Cart removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = requireOwnedItem(cart, cartItemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return cartRepository.findByUserId(userId).orElseThrow();
    }

    public void clear(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
    }

    private CartItem requireOwnedItem(Cart cart, Long cartItemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("CartItem", cartItemId));
    }
}
