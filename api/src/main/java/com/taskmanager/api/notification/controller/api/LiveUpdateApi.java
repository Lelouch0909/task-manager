package com.taskmanager.api.notification.controller.api;
import com.taskmanager.api.notification.service.impl.LiveUpdateServiceImpl.Sync;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Temps réel") @SecurityRequirement(name = "bearerAuth")
public interface LiveUpdateApi {
    @GetMapping(value = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Recevoir les changements via SSE", description = "Flux personnel Bearer. Événement sync : {tasks: boolean, notifications: boolean}. Recharger les ressources REST indiquées. Un sync complet est envoyé à chaque connexion : pas de replay Last-Event-ID. Heartbeat toutes les 15 secondes. Le flux ferme à l’expiration du JWT ; le client renouvelle puis se reconnecte. Révocation contrôlée avant chaque envoi. Huit connexions par compte par défaut.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flux SSE.", content = @Content(mediaType = "text/event-stream", schema = @Schema(implementation = Sync.class))),
        @ApiResponse(responseCode = "401", description = "Token invalide ou expiré.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Limite de connexions atteinte.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<SseEmitter> subscribe(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt);
}
