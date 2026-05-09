package com.cinema.notification;

import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.notification.NotificationDtos.NotificationListResponse;
import com.cinema.notification.NotificationDtos.NotificationResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    private final NotificationService notificationService;

    public NotificationController(NotificationRepository notifications, NotificationService notificationService) {
        this.notifications = notifications;
        this.notificationService = notificationService;
    }

    @GetMapping
    NotificationListResponse list(@AuthenticationPrincipal AuthUser user) {
        return new NotificationListResponse(notifications.countByUserIdAndReadAtIsNull(user.id()),
                notifications.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(NotificationResponse::from).toList());
    }

    @PostMapping("/{id}/read")
    void read(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        notificationService.markRead(user.id(), id);
    }

    @PostMapping("/read-all")
    void readAll(@AuthenticationPrincipal AuthUser user) {
        notificationService.markAllRead(user.id());
    }
}
