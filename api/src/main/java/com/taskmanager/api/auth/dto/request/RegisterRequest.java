package com.taskmanager.api.auth.dto.request;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record RegisterRequest(
    @NotBlank @Size(max = 100) @Schema(example = "Alice", maxLength = 100) String displayName,
    @NotBlank @Email @Size(max = 254) @Schema(example = "alice@example.com") String email,
    @NotBlank @Size(min = 8, max = 72)
    @Schema(description = "8 caractères minimum, 72 octets UTF-8 maximum.", accessMode = Schema.AccessMode.WRITE_ONLY)
    String password
) {}
