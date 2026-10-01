package com.tailoredplatform.ecommerce.orders.mapper;

import com.tailoredplatform.ecommerce.orders.dto.OrderItemResponse;
import com.tailoredplatform.ecommerce.orders.dto.OrderResponse;
import com.tailoredplatform.ecommerce.orders.dto.OrderTimelineEntryResponse;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    private static final List<Order.Status> HAPPY_PATH = List.of(
            Order.Status.PENDING, Order.Status.CONFIRMED, Order.Status.PROCESSING,
            Order.Status.SHIPPED, Order.Status.DELIVERED
    );

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                items,
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getShippingFee(),
                order.getTaxTotal(),
                order.getGrandTotal(),
                order.getCurrency(),
                order.getPlacedAt(),
                order.isCancellable(),
                buildTimeline(order)
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductNameSnapshot(),
                item.getSkuSnapshot(),
                item.getVariantLabelSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }

    /**
     * A cancelled/refunded order shows a short two-step timeline (placed ->
     * cancelled/refunded) rather than the full happy path with everything
     * after the cancellation point misleadingly marked incomplete.
     */
    private List<OrderTimelineEntryResponse> buildTimeline(Order order) {
        if (order.getStatus() == Order.Status.CANCELLED) {
            return List.of(
                    new OrderTimelineEntryResponse("PENDING", "Order placed", order.getPlacedAt(), true),
                    new OrderTimelineEntryResponse("CANCELLED", "Order cancelled", order.getCancelledAt(), true)
            );
        }
        if (order.getStatus() == Order.Status.REFUNDED) {
            return List.of(
                    new OrderTimelineEntryResponse("PENDING", "Order placed", order.getPlacedAt(), true),
                    new OrderTimelineEntryResponse("DELIVERED", "Delivered", null, true),
                    new OrderTimelineEntryResponse("REFUNDED", "Refunded", order.getUpdatedAt(), true)
            );
        }

        int currentIndex = HAPPY_PATH.indexOf(order.getStatus());
        return HAPPY_PATH.stream()
                .map(status -> new OrderTimelineEntryResponse(
                        status.name(),
                        label(status),
                        status == Order.Status.PENDING ? order.getPlacedAt() : null,
                        HAPPY_PATH.indexOf(status) <= currentIndex
                ))
                .toList();
    }

    private String label(Order.Status status) {
        return switch (status) {
            case PENDING -> "Order placed";
            case CONFIRMED -> "Order confirmed";
            case PROCESSING -> "Preparing your order";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            case REFUNDED -> "Refunded";
        };
    }
}
