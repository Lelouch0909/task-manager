package com.taskmanager.api.auth.dto.request;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record EmailRequest(@NotBlank @Email @Size(max = 254) @Schema(example = "alice@example.com") String email) {}
