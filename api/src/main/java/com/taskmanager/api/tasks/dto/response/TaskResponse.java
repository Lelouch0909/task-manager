package com.taskmanager.api.tasks.dto.response;
import com.taskmanager.api.tasks.model.*;
import java.time.Instant;
import java.util.UUID;
public record TaskResponse(UUID id, String title, String description, TaskStatus status, Instant createdAt, Instant updatedAt) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
