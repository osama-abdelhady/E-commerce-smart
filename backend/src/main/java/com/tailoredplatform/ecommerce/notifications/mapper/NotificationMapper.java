package com.tailoredplatform.ecommerce.notifications.mapper;

import com.tailoredplatform.ecommerce.notifications.dto.NotificationResponse;
import com.tailoredplatform.ecommerce.notifications.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.isRead(), n.getCreatedAt());
    }
}
