package com.taskmanager.api.notification.service;
import com.taskmanager.api.notification.dto.response.*;
import java.util.UUID;
public interface NotificationService {
    NotificationPage list(UUID ownerId, int page, int size, boolean unreadOnly);
    UnreadCount unreadCount(UUID ownerId);
    NotificationResponse setRead(UUID ownerId, UUID id, boolean read);
    void markAllRead(UUID ownerId);
}
