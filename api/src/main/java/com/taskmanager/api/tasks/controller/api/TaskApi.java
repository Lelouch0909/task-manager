package com.taskmanager.api.tasks.controller.api;
import com.taskmanager.api.tasks.dto.request.*;
import com.taskmanager.api.tasks.dto.response.*;
import com.taskmanager.api.tasks.model.TaskStatus;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RequestMapping("/api/tasks")
@Tag(name = "Tâches", description = "Toutes les opérations sont limitées au propriétaire authentifié.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Corps, statut, recherche ou pagination invalide.", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "401", description = "Token absent, invalide, expiré ou session révoquée.", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
})
public interface TaskApi {
    @GetMapping
    @Operation(summary = "Lister mes tâches", description = "Tri par createdAt puis id décroissants. Recherche littérale insensible à la casse dans titre et description, combinable avec le statut.")
    @ApiResponse(responseCode = "200", description = "Page de tâches ; liste vide si aucun résultat.")
    TaskPage list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
        @Parameter(description = "Index de page, à partir de zéro.", schema = @Schema(minimum = "0"))
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Taille de page, de 1 à 100.", schema = @Schema(minimum = "1", maximum = "100"))
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) TaskStatus status,
        @Parameter(description = "Recherche textuelle, 200 caractères maximum.") @RequestParam(required = false) String search);

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une tâche", description = "Le propriétaire est déduit du JWT. Le statut est TODO si absent.")
    @ApiResponse(responseCode = "201", description = "Tâche créée.")
    TaskResponse create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTaskRequest request);

    @PutMapping("/{id}")
    @Operation(summary = "Remplacer une tâche", description = "Remplace titre, description et statut. Une description absente efface sa valeur. Dernière écriture gagnante.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Tâche modifiée."),
        @ApiResponse(responseCode = "404", description = "Tâche inexistante ou appartenant à un autre compte.")})
    TaskResponse update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request);

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une tâche", description = "Suppression définitive.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Tâche supprimée."),
        @ApiResponse(responseCode = "404", description = "Tâche inexistante ou appartenant à un autre compte.")})
    void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id);
}
