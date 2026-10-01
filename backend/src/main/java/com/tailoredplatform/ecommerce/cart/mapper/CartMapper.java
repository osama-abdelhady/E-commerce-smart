package com.tailoredplatform.ecommerce.cart.mapper;

import com.tailoredplatform.ecommerce.cart.dto.CartItemResponse;
import com.tailoredplatform.ecommerce.cart.dto.CartResponse;
import com.tailoredplatform.ecommerce.cart.entity.Cart;
import com.tailoredplatform.ecommerce.cart.entity.CartItem;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * unitPriceSnapshot on CartItem is for display only (see the entity's
 * javadoc) — this mapper compares it against the variant's CURRENT
 * effectivePrice() and flags priceChanged so the frontend can show
 * "price updated since you added this" rather than silently charging
 * a different amount at checkout.
 */
@Component
@RequiredArgsConstructor
public class CartMapper {

    private final InventoryRepository inventoryRepository;

    public CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream().map(this::toItemResponse).toList();
        int itemCount = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, itemCount, subtotal);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        var variant = item.getProductVariant();
        var product = variant.getProduct();
        BigDecimal currentPrice = variant.effectivePrice();
        boolean priceChanged = currentPrice.compareTo(item.getUnitPriceSnapshot()) != 0;

        int available = inventoryRepository.findByProductVariantId(variant.getId())
                .map(inv -> inv.getQuantityAvailable() - inv.getQuantityReserved())
                .orElse(0);

        String imageUrl = product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl();

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                product.getName(),
                product.getSlug(),
                imageUrl,
                variant.getSize(),
                variant.getColor(),
                currentPrice,
                priceChanged,
                item.getQuantity(),
                currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
                available >= item.getQuantity(),
                available
        );
    }
}
