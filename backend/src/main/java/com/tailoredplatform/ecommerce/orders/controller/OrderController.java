package com.tailoredplatform.ecommerce.orders.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.orders.dto.CheckoutRequest;
import com.tailoredplatform.ecommerce.orders.dto.OrderResponse;
import com.tailoredplatform.ecommerce.orders.dto.UpdateOrderStatusRequest;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.mapper.OrderMapper;
import com.tailoredplatform.ecommerce.orders.service.OrderService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, order history, and order management")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from the current cart (reserves inventory; payment confirms it)")
    public OrderResponse checkout(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CheckoutRequest request) {
        Order order = orderService.checkout(principal.getUser(), request);
        return orderMapper.toResponse(order);
    }

    @GetMapping
    @Operation(summary = "List the current user's order history, newest first")
    public PageResponse<OrderResponse> myOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "placedAt"));
        var orders = orderService.listForUser(principal.getId(), pageable);
        return PageResponse.from(orders.map(orderMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Order detail, including status timeline")
    public OrderResponse getOrder(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderMapper.toResponse(orderService.getOwnedOrder(principal.getUser(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an eligible order (PENDING or CONFIRMED only)")
    public OrderResponse cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Cancelled by customer") String reason
    ) {
        return orderMapper.toResponse(orderService.cancel(principal.getUser(), id, reason));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: transition an order's status")
    public OrderResponse updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        Order.Status newStatus = Order.Status.valueOf(request.status().toUpperCase());
        return orderMapper.toResponse(orderService.adminUpdateStatus(principal.getUser(), id, newStatus, request.note()));
    }
}
