package com.tailoredplatform.ecommerce.orders.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.orders.dto.OrderResponse;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.mapper.OrderMapper;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Orders", description = "All orders — search by number/customer, filter by date and status")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @GetMapping
    @Operation(summary = "List/search/filter all orders")
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (orderNumber != null && !orderNumber.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("orderNumber")), "%" + orderNumber.toLowerCase() + "%"));
            }
            if (customerEmail != null && !customerEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("user").get("email")), "%" + customerEmail.toLowerCase() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Order.Status.valueOf(status.toUpperCase())));
            }
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("placedAt"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("placedAt"), toDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "placedAt"));
        var result = orderRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(orderMapper::toResponse));
    }
}
