package com.tailoredplatform.ecommerce.shipping.controller;

import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.orders.service.OrderService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import com.tailoredplatform.ecommerce.shipping.dto.ShipmentResponse;
import com.tailoredplatform.ecommerce.shipping.repository.ShipmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/shipment")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Shipment tracking for an order")
public class ShippingController {

    private final ShipmentRepository shipmentRepository;
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get shipment/tracking info for an order you own")
    public ShipmentResponse getShipment(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        var order = orderService.getOwnedOrder(principal.getUser(), orderId); // ownership check
        var shipment = shipmentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("This order has not shipped yet."));

        return new ShipmentResponse(
                shipment.getCarrier(), shipment.getTrackingNumber(), shipment.getStatus().name(),
                shipment.getShippedAt(), shipment.getDeliveredAt(), shipment.getEstimatedDeliveryAt()
        );
    }
}
