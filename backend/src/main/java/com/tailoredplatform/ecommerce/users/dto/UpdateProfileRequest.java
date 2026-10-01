package com.tailoredplatform.ecommerce.users.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Size(max = 30) String phone
) {
}
