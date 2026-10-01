package com.tailoredplatform.ecommerce.users.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.orders.dto.OrderResponse;
import com.tailoredplatform.ecommerce.orders.mapper.OrderMapper;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import com.tailoredplatform.ecommerce.users.dto.CustomerSummaryResponse;
import com.tailoredplatform.ecommerce.users.dto.UserResponse;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.users.mapper.UserMapper;
import com.tailoredplatform.ecommerce.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Customers", description = "Customer account search, detail, activation")
public class AdminCustomerController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    @GetMapping
    @Operation(summary = "List/search customers by email or name")
    public PageResponse<CustomerSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = (search == null || search.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable);

        var mapped = result.map(u -> new CustomerSummaryResponse(
                u.getId(), u.getEmail(), u.getFullName(), u.getStatus().name(),
                u.getCreatedAt(), orderRepository.countByUserId(u.getId())
        ));
        return PageResponse.from(mapped);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer's profile")
    public UserResponse getById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
        return userMapper.toResponse(user);
    }

    @GetMapping("/{id}/orders")
    @Operation(summary = "Get a customer's order history")
    public PageResponse<OrderResponse> getOrders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "placedAt"));
        var orders = orderRepository.findByUserId(id, pageable);
        return PageResponse.from(orders.map(orderMapper::toResponse));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reactivate a suspended/deactivated customer account")
    public UserResponse activate(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
        user.setStatus(User.Status.ACTIVE);
        return userMapper.toResponse(userRepository.save(user));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a customer account")
    public UserResponse deactivate(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
        user.setStatus(User.Status.DEACTIVATED);
        return userMapper.toResponse(userRepository.save(user));
    }
}
