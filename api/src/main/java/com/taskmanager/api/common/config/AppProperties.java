package com.taskmanager.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Validated
@ConfigurationProperties("app")
public record AppProperties(
    @NotBlank @Size(min = 32) String jwtSecret,
    @NotBlank @Size(min = 32) String codeSecret,
    List<String> allowedOrigins,
    boolean cookieSecure,
    String resendApiKey,
    String resendFrom,
    String resendBaseUrl
) {}
