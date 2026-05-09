package com.cinema.notification;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(UUID id, String type, String title, String message, boolean read, Instant readAt, Instant createdAt) {
        public static NotificationResponse from(Notification notification) {
            return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                    notification.getMessage(), notification.getReadAt() != null, notification.getReadAt(), notification.getCreatedAt());
        }
    }

    public record NotificationListResponse(long unreadCount, java.util.List<NotificationResponse> notifications) {
    }
}
