package com.taskmanager.api.auth.controller;

import com.taskmanager.api.auth.controller.api.AuthApi;
import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import com.taskmanager.api.auth.service.*;
import com.taskmanager.api.common.config.AppProperties;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import java.time.*;
import java.util.*;

@RestController @RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService service;
    private final RateLimitService limits;
    private final SecretService secrets;
    private final AppProperties properties;
    private final Clock clock;

    public UserResponse register(RegisterRequest body, HttpServletRequest request) {
        limit(request, "register", 10); return service.register(body);
    }
    public void verify(VerifyEmailRequest body, HttpServletRequest request) {
        limit(request, "verify", 30); service.verify(body);
    }
    public void resend(EmailRequest body, HttpServletRequest request) {
        limit(request, "email", 10); service.resend(body);
    }
    public void forgot(EmailRequest body, HttpServletRequest request) {
        limit(request, "email", 10); service.forgot(body);
    }
    public void reset(ResetPasswordRequest body, HttpServletRequest request) {
        limit(request, "reset", 30); service.reset(body);
    }
    public LoginResponse login(LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        limit(request, "login", 30);
        limits.check("login-account:" + secrets.tokenHash(body.email().strip().toLowerCase(Locale.ROOT)), 10);
        return setSession(service.login(body), response);
    }
    public LoginResponse refresh(String token, HttpServletRequest request, HttpServletResponse response) {
        limit(request, "refresh", 60);
        try { return setSession(service.refresh(token), response); }
        catch (com.taskmanager.api.common.exception.ApiException ex) {
            cookie(response, "", Duration.ZERO); throw ex;
        }
    }
    public void logout(String token, HttpServletResponse response) {
        service.logout(token); cookie(response, "", Duration.ZERO);
    }
    public UserResponse me(Jwt jwt) { return service.me(UUID.fromString(jwt.getSubject())); }
    private void limit(HttpServletRequest request, String operation, int maximum) {
        // Do not trust arbitrary X-Forwarded-For headers.
        limits.check(operation + ":" + request.getRemoteAddr(), maximum);
    }
    private LoginResponse setSession(AuthService.SessionResult session, HttpServletResponse response) {
        cookie(response, session.refreshToken(), Duration.between(clock.instant(), session.expiresAt()));
        return session.response();
    }
    private void cookie(HttpServletResponse response, String value, Duration maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("refresh_token", value)
            .httpOnly(true).secure(properties.cookieSecure()).sameSite("Lax").path("/api/auth")
            .maxAge(maxAge).build().toString());
    }
}
