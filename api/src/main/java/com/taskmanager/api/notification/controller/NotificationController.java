package com.taskmanager.api.notification.controller;
import com.taskmanager.api.notification.controller.api.NotificationApi;
import com.taskmanager.api.notification.dto.request.ReadNotificationRequest;
import com.taskmanager.api.notification.dto.response.*;
import com.taskmanager.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class NotificationController implements NotificationApi {
    private final NotificationService service;
    public NotificationPage list(Jwt jwt, int page, int size, boolean unreadOnly) {
        return service.list(UUID.fromString(jwt.getSubject()), page, size, unreadOnly);
    }
    public UnreadCount count(Jwt jwt) { return service.unreadCount(UUID.fromString(jwt.getSubject())); }
    public NotificationResponse setRead(Jwt jwt, UUID id, ReadNotificationRequest request) {
        return service.setRead(UUID.fromString(jwt.getSubject()), id, request.read());
    }
    public void readAll(Jwt jwt) { service.markAllRead(UUID.fromString(jwt.getSubject())); }
}
