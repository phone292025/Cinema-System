package com.cinema.notification;

import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.notification.NotificationDtos.NotificationListResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    NotificationListResponse list(@AuthenticationPrincipal AuthUser user) {
        return notificationService.list(user.id());
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
