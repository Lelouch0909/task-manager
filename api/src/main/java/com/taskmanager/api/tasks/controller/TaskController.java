package com.taskmanager.api.tasks.controller;
import com.taskmanager.api.tasks.controller.api.TaskApi;
import com.taskmanager.api.tasks.dto.request.*;
import com.taskmanager.api.tasks.dto.response.*;
import com.taskmanager.api.tasks.model.TaskStatus;
import com.taskmanager.api.tasks.service.TaskService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class TaskController implements TaskApi {
    private final TaskService service;
    public TaskPage list(Jwt jwt, int page, int size, TaskStatus status, String search) {
        return service.list(UUID.fromString(jwt.getSubject()), page, size, status, search);
    }
    public TaskResponse create(Jwt jwt, CreateTaskRequest request) { return service.create(UUID.fromString(jwt.getSubject()), request); }
    public TaskResponse update(Jwt jwt, UUID id, UpdateTaskRequest request) { return service.update(UUID.fromString(jwt.getSubject()), id, request); }
    public void delete(Jwt jwt, UUID id) { service.delete(UUID.fromString(jwt.getSubject()), id); }
}
