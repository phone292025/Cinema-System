package com.cinema.notification;

import java.time.Instant;
import java.util.UUID;

import com.cinema.common.ApiException;
import com.cinema.notification.NotificationDtos.NotificationListResponse;
import com.cinema.notification.NotificationDtos.NotificationResponse;
import com.cinema.user.User;
import com.cinema.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID userId) {
        return new NotificationListResponse(notifications.countByUserIdAndReadAtIsNull(userId),
                notifications.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList());
    }

    @Transactional
    public Notification create(UUID userId, String type, String title, String message) {
        return create(userId, type, title, message, null);
    }

    @Transactional
    public Notification create(UUID userId, String type, String title, String message, UUID bookingId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setBookingId(bookingId);
        return notifications.save(notification);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found."));
        if (notification.getReadAt() == null) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notifications.markAllRead(userId, Instant.now());
    }
}
