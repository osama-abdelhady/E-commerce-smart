package com.tailoredplatform.ecommerce.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 50) String label,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank String line1,
        String line2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String state,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Size(max = 30) String phone,
        boolean isDefault
) {
}
