package com.tailoredplatform.ecommerce.wishlist.mapper;

import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.wishlist.dto.WishlistItemResponse;
import com.tailoredplatform.ecommerce.wishlist.dto.WishlistResponse;
import com.tailoredplatform.ecommerce.wishlist.entity.Wishlist;
import com.tailoredplatform.ecommerce.wishlist.entity.WishlistItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WishlistMapper {

    private final InventoryRepository inventoryRepository;

    public WishlistResponse toResponse(Wishlist wishlist) {
        var items = wishlist.getItems().stream().map(this::toItemResponse).toList();
        return new WishlistResponse(wishlist.getId(), items);
    }

    private WishlistItemResponse toItemResponse(WishlistItem item) {
        var product = item.getProduct();
        boolean inStock = product.getVariants().stream().anyMatch(v ->
                inventoryRepository.findByProductVariantId(v.getId())
                        .map(inv -> (inv.getQuantityAvailable() - inv.getQuantityReserved()) > 0)
                        .orElse(false));
        String imageUrl = product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl();

        return new WishlistItemResponse(
                item.getId(), product.getId(), product.getName(), product.getSlug(),
                imageUrl, product.getPrice(), product.getDiscountPrice(), inStock
        );
    }
}
