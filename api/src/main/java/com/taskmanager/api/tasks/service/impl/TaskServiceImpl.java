package com.taskmanager.api.tasks.service.impl;
import com.taskmanager.api.tasks.service.TaskService;
import com.taskmanager.api.tasks.TaskChanged;
import org.springframework.context.ApplicationEventPublisher;
import com.taskmanager.api.tasks.dto.request.*;
import com.taskmanager.api.tasks.dto.response.*;
import com.taskmanager.api.tasks.model.*;
import com.taskmanager.api.tasks.repository.TaskRepository;
import com.taskmanager.api.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import java.time.Clock;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class TaskServiceImpl implements TaskService {
    private final TaskRepository tasks;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    @Transactional(readOnly = true)
    public TaskPage list(UUID ownerId, int page, int size, TaskStatus status, String search) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new ApiException(400, "invalid_pagination", "Pagination invalide ; taille autorisée de 1 à 100.");
        Specification<Task> spec = (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (search != null && !search.isBlank()) {
            if (search.length() > 200) throw new ApiException(400, "invalid_search", "Recherche limitée à 200 caractères.");
            String term = "%" + search.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("title")), term, '!'),
                cb.like(cb.lower(root.get("description")), term, '!')));
        }
        var result = tasks.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new TaskPage(result.stream().map(TaskResponse::from).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }
    public TaskResponse create(UUID ownerId, CreateTaskRequest request) {
        var task = tasks.save(new Task(ownerId, request.title(), request.description(), request.status(), clock.instant()));
        changed(task, TaskChanged.Kind.CREATED);
        return TaskResponse.from(task);
    }
    public TaskResponse update(UUID ownerId, UUID id, UpdateTaskRequest request) {
        var task = owned(ownerId, id);
        var previousStatus = task.getStatus();
        task.update(request.title(), request.description(), request.status(), clock.instant());
        changed(task, previousStatus == task.getStatus() ? TaskChanged.Kind.UPDATED : TaskChanged.Kind.STATUS_CHANGED);
        return TaskResponse.from(task);
    }
    public void delete(UUID ownerId, UUID id) {
        var task = owned(ownerId, id);
        tasks.delete(task);
        changed(task, TaskChanged.Kind.DELETED);
    }
    private void changed(Task task, TaskChanged.Kind kind) {
        events.publishEvent(new TaskChanged(task.getOwnerId(), task.getId(), task.getTitle(), kind));
    }
    private Task owned(UUID ownerId, UUID id) {
        return tasks.findByIdAndOwnerId(id, ownerId).orElseThrow(com.taskmanager.api.tasks.exception.TaskNotFoundException::new);
    }
}
