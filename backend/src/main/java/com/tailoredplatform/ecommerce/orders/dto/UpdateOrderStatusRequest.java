package com.tailoredplatform.ecommerce.orders.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull String status,
        String note
) {
}
