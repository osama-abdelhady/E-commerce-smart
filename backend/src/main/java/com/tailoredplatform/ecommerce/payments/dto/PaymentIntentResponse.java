package com.tailoredplatform.ecommerce.payments.dto;

import java.math.BigDecimal;

public record PaymentIntentResponse(
        String clientSecret,
        BigDecimal amount,
        String currency,
        String orderNumber
) {
}
