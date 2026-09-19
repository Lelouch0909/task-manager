package com.taskmanager.api.auth.service;
import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import java.time.Instant;
import java.util.UUID;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    void verify(VerifyEmailRequest request);
    void resend(EmailRequest request);
    void forgot(EmailRequest request);
    void reset(ResetPasswordRequest request);
    SessionResult login(LoginRequest request);
    SessionResult refresh(String token);
    void logout(String token);
    UserResponse me(UUID userId);
    record SessionResult(LoginResponse response, String refreshToken, Instant expiresAt) {}
}
