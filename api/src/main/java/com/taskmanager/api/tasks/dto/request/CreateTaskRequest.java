package com.taskmanager.api.tasks.dto.request;
import com.taskmanager.api.tasks.model.TaskStatus;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record CreateTaskRequest(
    @NotBlank @Schema(description = "Titre de 1 à 200 caractères après suppression des espaces périphériques.", example = "Préparer la présentation") String title,
    @Size(max = 5000) @Schema(description = "Description facultative.", maxLength = 5000) String description,
    @Schema(description = "TODO par défaut.", defaultValue = "TODO") TaskStatus status
) {}
