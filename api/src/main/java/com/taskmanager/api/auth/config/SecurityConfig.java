package com.taskmanager.api.auth.config;

import com.taskmanager.api.auth.repository.AuthSessionRepository;
import com.taskmanager.api.common.config.AppProperties;
import com.taskmanager.api.common.exception.ApiExceptionHandler;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.io.IOException;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean JwtEncoder jwtEncoder(AppProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(properties.jwtSecret().getBytes(StandardCharsets.UTF_8)));
    }
    @Bean JwtDecoder jwtDecoder(AppProperties properties, AuthSessionRepository sessions, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
            properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> sessionValidator = jwt -> {
            try {
                UUID sid = UUID.fromString(jwt.getClaimAsString("sid"));
                UUID uid = UUID.fromString(jwt.getSubject());
                if (jwt.getExpiresAt() != null && jwt.getAudience().contains("taskmanager-api")
                    && sessions.findById(sid).filter(s -> s.getUserId().equals(uid) && s.active(clock.instant())).isPresent())
                    return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException | NullPointerException ex) { /* invalid claims */ }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Session invalide ou expirée.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, new JwtIssuerValidator("taskmanager"), sessionValidator));
        return decoder;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties, ObjectMapper mapper) throws Exception {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        http.cors(c -> c.configurationSource(source))
            .csrf(c -> c.disable()) // Cookie endpoints are explicitly protected by Origin/Fetch-Metadata checks below.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health/readiness", "/actuator/health/liveness").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                    "/api/auth/logout", "/api/auth/email/verify", "/api/auth/email/resend",
                    "/api/auth/password/forgot", "/api/auth/password/reset").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> {}).authenticationEntryPoint((req, res, ex) -> write(mapper, req, res, 401, "unauthorized", "Authentification requise ou token invalide.")))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(mapper, req, res, 401, "unauthorized", "Authentification requise."))
                .accessDeniedHandler((req, res, ex) -> write(mapper, req, res, 403, "forbidden", "Accès refusé.")))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                        throws ServletException, IOException {
                    if (request.getRequestURI().startsWith("/api/auth/")) {
                        response.setHeader("Cache-Control", "no-store");
                        if ("POST".equals(request.getMethod())) {
                            String origin = request.getHeader("Origin");
                            String site = request.getHeader("Sec-Fetch-Site");
                            if ((origin != null && !properties.allowedOrigins().contains(origin))
                                || (origin == null && site != null && !"same-origin".equals(site) && !"none".equals(site))) {
                                write(mapper, request, response, 403, "origin_not_allowed", "Origine de la requête non autorisée.");
                                return;
                            }
                        }
                    }
                    chain.doFilter(request, response);
                }
            }, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
    private static void write(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res,
                              int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), ApiExceptionHandler.problem(status, code, message, req.getRequestURI()));
    }
}
