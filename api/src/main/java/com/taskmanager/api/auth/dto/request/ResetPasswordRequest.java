package com.taskmanager.api.auth.dto.request;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record ResetPasswordRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Pattern(regexp = "[0-9]{6}") @Schema(example = "123456") String code,
    @NotBlank @Size(min = 8, max = 72)
    @Schema(description = "8 caractères minimum, 72 octets UTF-8 maximum.", accessMode = Schema.AccessMode.WRITE_ONLY)
    String password
) {}
