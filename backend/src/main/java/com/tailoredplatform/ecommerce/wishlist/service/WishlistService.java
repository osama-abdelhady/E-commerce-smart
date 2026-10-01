package com.tailoredplatform.ecommerce.wishlist.service;

import com.tailoredplatform.ecommerce.cart.service.CartService;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.wishlist.entity.Wishlist;
import com.tailoredplatform.ecommerce.wishlist.entity.WishlistItem;
import com.tailoredplatform.ecommerce.wishlist.repository.WishlistItemRepository;
import com.tailoredplatform.ecommerce.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public Wishlist getOrCreate(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wishlist not found for user " + userId + ". This should not happen for a registered account."));
    }

    public Wishlist addItem(Long userId, Long productId) {
        Wishlist wishlist = getOrCreate(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        if (wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId).isEmpty()) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlistItemRepository.save(item);
            wishlist.getItems().add(item);
        }
        return wishlist;
    }

    public Wishlist removeItem(Long userId, Long productId) {
        Wishlist wishlist = getOrCreate(userId);
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
        wishlist.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return wishlist;
    }

    /**
     * Moves a wishlisted product into the cart. Since a wishlist item is
     * product-level (no size/color chosen yet) but the cart is variant-level,
     * the caller must specify which variant to add — this picks the first
     * active, in-stock variant unless one is given.
     */
    public Wishlist moveToCart(Long userId, Long productId, Long variantId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        ProductVariant target = variantId != null
                ? product.getVariants().stream().filter(v -> v.getId().equals(variantId)).findFirst()
                        .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", variantId))
                : product.getVariants().stream().filter(ProductVariant::isActive).findFirst()
                        .orElseThrow(() -> new BusinessRuleViolationException("This product has no available variants."));

        cartService.addItem(userId, target.getId(), quantity);
        return removeItem(userId, productId);
    }
}
