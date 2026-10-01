package com.tailoredplatform.ecommerce.notifications.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        boolean isRead,
        Instant createdAt
) {
}
