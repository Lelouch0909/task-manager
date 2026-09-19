package com.taskmanager.api.auth.service.impl;
import com.taskmanager.api.auth.service.*;
import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import com.taskmanager.api.auth.model.CodePurpose;
import com.taskmanager.api.notification.service.EmailService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthStateService state;
    private final EmailService emails;
    public UserResponse register(RegisterRequest request) {
        var dispatch = state.register(request); // Transaction committed before contacting Resend.
        send(dispatch);
        return dispatch.user();
    }
    public void verify(VerifyEmailRequest request) { state.verify(request); }
    public void resend(EmailRequest request) { send(state.requestCode(request.email(), CodePurpose.VERIFY_EMAIL)); }
    public void forgot(EmailRequest request) { send(state.requestCode(request.email(), CodePurpose.RESET_PASSWORD)); }
    public void reset(ResetPasswordRequest request) { state.reset(request); }
    public SessionResult login(LoginRequest request) { return state.login(request); }
    public SessionResult refresh(String token) { return state.refresh(token); }
    public void logout(String token) { state.logout(token); }
    public UserResponse me(UUID userId) { return state.me(userId); }
    private void send(AuthStateService.CodeDispatch dispatch) {
        if (dispatch == null) return;
        if (dispatch.purpose() == CodePurpose.VERIFY_EMAIL) emails.sendVerification(dispatch.user().email(), dispatch.code());
        else emails.sendPasswordReset(dispatch.user().email(), dispatch.code());
    }
}
