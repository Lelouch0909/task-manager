package com.taskmanager.api.notification.service.impl;
import com.taskmanager.api.notification.service.NotificationService;
import com.taskmanager.api.notification.dto.response.*;
import com.taskmanager.api.notification.repository.NotificationRepository;
import com.taskmanager.api.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.time.Clock;

@Service @RequiredArgsConstructor @Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public NotificationPage list(UUID ownerId, int page, int size, boolean unreadOnly) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new ApiException(400, "invalid_pagination", "Pagination invalide ; taille autorisée de 1 à 100.");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = unreadOnly ? repository.findByOwnerIdAndReadAtIsNull(ownerId, pageable)
            : repository.findByOwnerId(ownerId, pageable);
        return new NotificationPage(result.map(NotificationResponse::from).toList(), page, size,
            result.getTotalElements(), result.getTotalPages(), repository.countByOwnerIdAndReadAtIsNull(ownerId));
    }
    @Transactional(readOnly = true)
    public UnreadCount unreadCount(UUID ownerId) {
        return new UnreadCount(repository.countByOwnerIdAndReadAtIsNull(ownerId));
    }
    public NotificationResponse setRead(UUID ownerId, UUID id, boolean read) {
        var notification = repository.findOwnedForUpdate(ownerId, id)
            .orElseThrow(() -> new ApiException(404, "notification_not_found", "Notification introuvable."));
        if (notification.setRead(read, clock.instant())) events.publishEvent(new LiveUpdate(ownerId, false, true));
        return NotificationResponse.from(notification);
    }
    public void markAllRead(UUID ownerId) {
        if (repository.markAllRead(ownerId, clock.instant()) > 0) events.publishEvent(new LiveUpdate(ownerId, false, true));
    }
}
