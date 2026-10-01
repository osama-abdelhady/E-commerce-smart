package com.tailoredplatform.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RestockRequest(
        @Min(1) int quantity,
        @NotBlank String reason
) {
}
