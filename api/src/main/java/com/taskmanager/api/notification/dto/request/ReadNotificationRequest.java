package com.taskmanager.api.notification.dto.request;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
public record ReadNotificationRequest(
    @NotNull @Schema(description = "true : lue ; false : non lue.", example = "true") Boolean read
) {}
