package com.tailoredplatform.ecommerce.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long shippingAddressId,
        @NotNull Long billingAddressId,
        String couponCode,

        /**
         * Client-generated (e.g. a UUID created when the checkout page
         * loads). Prevents a double-tap on "Place Order" or a network
         * retry from creating two orders for the same cart — OrderService
         * short-circuits and returns the existing order for a repeated key.
         */
        @NotBlank String idempotencyKey
) {
}
