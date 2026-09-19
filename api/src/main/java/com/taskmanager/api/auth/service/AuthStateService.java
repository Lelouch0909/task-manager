package com.taskmanager.api.auth.service;
import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import com.taskmanager.api.auth.model.CodePurpose;
import java.util.UUID;

public interface AuthStateService {
    CodeDispatch register(RegisterRequest request);
    CodeDispatch requestCode(String email, CodePurpose purpose);
    void verify(VerifyEmailRequest request);
    void reset(ResetPasswordRequest request);
    AuthService.SessionResult login(LoginRequest request);
    AuthService.SessionResult refresh(String token);
    void logout(String token);
    UserResponse me(UUID userId);
    record CodeDispatch(UserResponse user, String code, CodePurpose purpose) {}
}
