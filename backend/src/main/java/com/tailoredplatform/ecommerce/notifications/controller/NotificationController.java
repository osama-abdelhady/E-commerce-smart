package com.tailoredplatform.ecommerce.notifications.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.notifications.dto.NotificationResponse;
import com.tailoredplatform.ecommerce.notifications.mapper.NotificationMapper;
import com.tailoredplatform.ecommerce.notifications.service.NotificationService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @GetMapping
    @Operation(summary = "List the current user's notifications, newest first")
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        var pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        var notifications = notificationService.listForUser(principal.getId(), pageable);
        return PageResponse.from(notifications.map(notificationMapper::toResponse));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count, for a navbar badge")
    public long unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.unreadCount(principal.getId());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public NotificationResponse markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return notificationMapper.toResponse(notificationService.markRead(principal.getUser(), id));
    }
}
