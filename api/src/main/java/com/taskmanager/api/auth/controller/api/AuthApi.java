package com.taskmanager.api.auth.controller.api;

import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Comptes locaux, vérification email et sessions révocables.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Champs invalides ou code invalide/expiré/épuisé.", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "429", description = "Limite de tentatives ou d’envoi atteinte.", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "503", description = "Email indisponible. Un compte créé reste enregistré ; utiliser le renvoi après le cooldown.", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
})
public interface AuthApi {
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte", description = "Crée le compte puis envoie un code valable 10 minutes. Aucun token émis avant vérification. En cas de 503, le compte est conservé ; utiliser /email/resend.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Compte créé et email accepté par le fournisseur, livraison non garantie."),
        @ApiResponse(responseCode = "409", description = "Adresse déjà utilisée.")})
    UserResponse register(@Valid @RequestBody RegisterRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @PostMapping("/email/verify") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Vérifier l’adresse email", description = "Code à six chiffres, usage unique, cinq essais maximum ; aucune connexion automatique.")
    @ApiResponse(responseCode = "204", description = "Adresse vérifiée.")
    void verify(@Valid @RequestBody VerifyEmailRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @PostMapping("/email/resend") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Renvoyer le code de vérification", description = "Réponse générique pour un compte inconnu ou déjà vérifié. 60 secondes entre envois, cinq par heure. Le nouveau code invalide le précédent.")
    @ApiResponse(responseCode = "202", description = "Demande traitée, sans garantie de livraison.")
    void resend(@Valid @RequestBody EmailRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @PostMapping("/password/forgot") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Demander un code de récupération", description = "Réponse générique pour une adresse inconnue. Code distinct de la vérification, valable 10 minutes. 60 secondes entre envois, cinq par heure.")
    @ApiResponse(responseCode = "202", description = "Demande traitée, sans garantie de livraison.")
    void forgot(@Valid @RequestBody EmailRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @PostMapping("/password/reset") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Réinitialiser le mot de passe", description = "Consomme le code, change le mot de passe et révoque toutes les sessions immédiatement. Ne vérifie pas l’email et ne connecte pas l’utilisateur.")
    @ApiResponse(responseCode = "204", description = "Mot de passe remplacé.")
    void reset(@Valid @RequestBody ResetPasswordRequest body, @Parameter(hidden = true) HttpServletRequest request);

    @PostMapping("/login")
    @Operation(summary = "Se connecter", description = "Retourne un JWT Bearer valable 15 minutes et pose refresh_token (HttpOnly, SameSite=Lax ; Secure hors développement), valable au maximum 7 jours.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Session ouverte."),
        @ApiResponse(responseCode = "401", description = "Identifiants incorrects."),
        @ApiResponse(responseCode = "403", description = "Email non vérifié ou origine interdite.")})
    LoginResponse login(@Valid @RequestBody LoginRequest body, @Parameter(hidden = true) HttpServletRequest request,
        @Parameter(hidden = true) HttpServletResponse response);

    @PostMapping("/refresh")
    @SecurityRequirement(name = "refreshCookie")
    @Operation(summary = "Renouveler la session", description = "Utilise le cookie HttpOnly et le remplace par rotation. L’échéance de 7 jours reste fixe. Réutiliser un ancien token révoque la session ; sérialiser les renouvellements côté client. Origin doit être autorisée pour un appel navigateur.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Tokens renouvelés."),
        @ApiResponse(responseCode = "401", description = "Session expirée, révoquée ou token réutilisé."),
        @ApiResponse(responseCode = "403", description = "Origine interdite.")})
    LoginResponse refresh(@Parameter(hidden = true) @CookieValue(name = "refresh_token", required = false) String token,
        @Parameter(hidden = true) HttpServletRequest request, @Parameter(hidden = true) HttpServletResponse response);

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "refreshCookie")
    @Operation(summary = "Fermer la session", description = "Révoque immédiatement la session et efface le cookie. Idempotent, même si le cookie est absent ou inconnu. Origin doit être autorisée pour un appel navigateur.")
    @ApiResponse(responseCode = "204", description = "Session fermée.")
    void logout(@Parameter(hidden = true) @CookieValue(name = "refresh_token", required = false) String token,
        @Parameter(hidden = true) HttpServletResponse response);

    @GetMapping("/me") @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consulter mon profil")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Profil connecté."),
        @ApiResponse(responseCode = "401", description = "Authentification requise.")})
    UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt);
}
