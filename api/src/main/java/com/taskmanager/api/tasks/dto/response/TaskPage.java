package com.taskmanager.api.tasks.dto.response;
import java.util.List;
public record TaskPage(List<TaskResponse> items, int page, int size, long totalElements, int totalPages) {}
