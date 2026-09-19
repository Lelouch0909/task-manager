package com.taskmanager.api.common.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Task Manager API").version("1.0")
            .description("Authentification locale, vérification email et gestion des tâches personnelles."))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                    .scheme("bearer").bearerFormat("JWT"))
                .addSecuritySchemes("refreshCookie", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE).name("refresh_token")));
    }
}
