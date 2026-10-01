package com.tailoredplatform.ecommerce.notifications.service;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.notifications.entity.Notification;
import com.tailoredplatform.ecommerce.notifications.repository.NotificationRepository;
import com.tailoredplatform.ecommerce.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification create(User user, Notification.Type type, String title, String body, String referenceType, Long referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type.name());
        notification.setTitle(title);
        notification.setBody(body);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Notification markRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleViolationException("This notification does not belong to the current user.");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}
