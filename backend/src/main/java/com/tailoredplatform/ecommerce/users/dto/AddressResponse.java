package com.tailoredplatform.ecommerce.users.dto;

public record AddressResponse(
        Long id,
        String label,
        String fullName,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        String phone,
        boolean isDefault
) {
}
