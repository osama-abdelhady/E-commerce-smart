package com.tailoredplatform.ecommerce.users.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String status,
        Set<String> roles
) {
}
