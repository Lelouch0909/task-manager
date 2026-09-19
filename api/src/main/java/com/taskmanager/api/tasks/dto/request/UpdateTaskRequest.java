package com.taskmanager.api.tasks.dto.request;
import com.taskmanager.api.tasks.model.TaskStatus;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record UpdateTaskRequest(
    @NotBlank @Schema(description = "Titre de 1 à 200 caractères après trim.", example = "Préparer la présentation") String title,
    @Size(max = 5000) @Schema(description = "Absente ou null : efface la description.") String description,
    @NotNull @Schema(description = "Statut obligatoire ; transitions libres.") TaskStatus status
) {}
