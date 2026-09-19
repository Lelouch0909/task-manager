package com.taskmanager.api.tasks.service;
import com.taskmanager.api.tasks.dto.request.*;
import com.taskmanager.api.tasks.dto.response.*;
import com.taskmanager.api.tasks.model.TaskStatus;
import java.util.UUID;
public interface TaskService {
    TaskPage list(UUID ownerId, int page, int size, TaskStatus status, String search);
    TaskResponse create(UUID ownerId, CreateTaskRequest request);
    TaskResponse update(UUID ownerId, UUID id, UpdateTaskRequest request);
    void delete(UUID ownerId, UUID id);
}
