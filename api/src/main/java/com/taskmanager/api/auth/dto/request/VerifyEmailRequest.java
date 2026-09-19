package com.taskmanager.api.auth.dto.request;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record VerifyEmailRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Pattern(regexp = "[0-9]{6}") @Schema(example = "123456") String code
) {}
