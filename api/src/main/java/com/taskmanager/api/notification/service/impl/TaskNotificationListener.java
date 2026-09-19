package com.taskmanager.api.notification.service.impl;
import com.taskmanager.api.tasks.TaskChanged;
import com.taskmanager.api.notification.model.Notification;
import com.taskmanager.api.notification.repository.NotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import java.time.Clock;

@Component @RequiredArgsConstructor
public class TaskNotificationListener {
    private final NotificationRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTaskChanged(TaskChanged event) {
        String prefix = switch (event.kind()) {
            case CREATED -> "Tâche créée : ";
            case UPDATED -> "Tâche modifiée : ";
            case STATUS_CHANGED -> "Statut modifié : ";
            case DELETED -> "Tâche supprimée : ";
        };
        repository.save(new Notification(event.ownerId(), event.taskId(), event.kind().name(),
            prefix + event.title(), clock.instant()));
        events.publishEvent(new LiveUpdate(event.ownerId(), true, true));
    }
}
