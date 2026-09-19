package com.taskmanager.api.notification.dto.response;
import com.taskmanager.api.notification.model.Notification;
import java.time.Instant;
import java.util.UUID;
public record NotificationResponse(UUID id, UUID taskId, String kind, String message, Instant createdAt, Instant readAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTaskId(), n.getKind(), n.getMessage(), n.getCreatedAt(), n.getReadAt());
    }
}
