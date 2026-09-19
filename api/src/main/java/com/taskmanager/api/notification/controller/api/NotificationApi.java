package com.taskmanager.api.notification.controller.api;
import com.taskmanager.api.notification.dto.request.ReadNotificationRequest;
import com.taskmanager.api.notification.dto.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import java.util.UUID;

@RequestMapping("/api/notifications") @SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Historique personnel persistant et état lu/non lu. Les mutations de tâches créent des notifications, y compris celles de l’utilisateur lui-même.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Requête ou pagination invalide.", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "401", description = "Authentification requise.", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
})
public interface NotificationApi {
    @GetMapping @Operation(summary = "Lister mes notifications", description = "Tri createdAt/id décroissants ; taille de 1 à 100. unreadCount porte sur toutes les notifications non lues.")
    @ApiResponse(responseCode = "200", description = "Page de notifications.")
    NotificationPage list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "false") boolean unreadOnly);

    @GetMapping("/unread-count") @Operation(summary = "Compter mes notifications non lues")
    @ApiResponse(responseCode = "200", description = "Compteur global.")
    UnreadCount count(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt);

    @PutMapping("/{id}/read") @Operation(summary = "Marquer une notification lue ou non lue", description = "Idempotent. Une notification d’un autre compte retourne 404.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "État actuel de la notification."),
        @ApiResponse(responseCode = "404", description = "Notification introuvable.")})
    NotificationResponse setRead(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id, @Valid @RequestBody ReadNotificationRequest request);

    @PutMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marquer toutes mes notifications comme lues", description = "Idempotent. Ne concerne jamais les notifications d’un autre compte.")
    @ApiResponse(responseCode = "204", description = "Notifications marquées lues.")
    void readAll(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt);
}
