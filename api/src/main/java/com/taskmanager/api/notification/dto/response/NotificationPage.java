package com.taskmanager.api.notification.dto.response;
import java.util.List;
public record NotificationPage(List<NotificationResponse> items, int page, int size, long totalElements, int totalPages, long unreadCount) {}
