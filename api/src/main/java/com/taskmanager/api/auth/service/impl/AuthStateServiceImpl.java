package com.taskmanager.api.auth.service.impl;

import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import com.taskmanager.api.auth.model.*;
import com.taskmanager.api.auth.repository.*;
import com.taskmanager.api.auth.service.*;
import com.taskmanager.api.auth.exception.*;
import com.taskmanager.api.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Invalid code attempts and replay revocations must be committed even when returning an error.
@Service @RequiredArgsConstructor @Transactional(noRollbackFor = ApiException.class)
public class AuthStateServiceImpl implements AuthStateService {
    private final UserAccountRepository users;
    private final AuthCodeRepository codes;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final SecretService secrets;
    private final TokenService tokens;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private static final String DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("unusable-dummy-password");

    public CodeDispatch register(RegisterRequest request) {
        validatePassword(request.password());
        String email = normalize(request.email());
        if (users.findByEmail(email).isPresent()) throw new ApiException(409, "email_already_used", "Cette adresse email est déjà utilisée.");
        var user = users.saveAndFlush(new UserAccount(request.displayName(), email, passwords.encode(request.password()), clock.instant()));
        return issueCode(user, CodePurpose.VERIFY_EMAIL);
    }
    public CodeDispatch requestCode(String email, CodePurpose purpose) {
        var user = users.lockByEmail(normalize(email)).orElse(null);
        if (user == null || (purpose == CodePurpose.VERIFY_EMAIL && user.isVerified())) return null;
        return issueCode(user, purpose);
    }
    private CodeDispatch issueCode(UserAccount user, CodePurpose purpose) {
        var code = secrets.newCode();
        var hash = secrets.codeHash(user.getId(), purpose, code);
        var now = clock.instant();
        var existing = codes.findByUserIdAndPurpose(user.getId(), purpose).orElse(null);
        if (existing == null) codes.save(new AuthCode(user.getId(), purpose, hash, now));
        else existing.reissue(hash, now);
        return new CodeDispatch(UserResponse.from(user), code, purpose);
    }
    public void verify(VerifyEmailRequest request) {
        var user = users.lockByEmail(normalize(request.email())).orElseThrow(this::invalidCode);
        consumeCode(user, CodePurpose.VERIFY_EMAIL, request.code());
        user.verify();
    }
    public void reset(ResetPasswordRequest request) {
        validatePassword(request.password());
        var user = users.lockByEmail(normalize(request.email())).orElseThrow(this::invalidCode);
        consumeCode(user, CodePurpose.RESET_PASSWORD, request.code());
        user.changePassword(passwords.encode(request.password()));
        sessions.findByUserId(user.getId()).forEach(AuthSession::revoke);
    }
    private void consumeCode(UserAccount user, CodePurpose purpose, String code) {
        var stored = codes.findByUserIdAndPurpose(user.getId(), purpose).orElseThrow(this::invalidCode);
        if (!stored.consume(secrets.codeHash(user.getId(), purpose, code), clock.instant())) throw invalidCode();
    }
    public AuthService.SessionResult login(LoginRequest request) {
        // BCrypt must never silently truncate multibyte passwords.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) throw invalidCredentials();
        var user = users.lockByEmail(normalize(request.email())).orElse(null);
        if (user == null) { passwords.matches(request.password(), DUMMY_HASH); throw invalidCredentials(); }
        if (!passwords.matches(request.password(), user.getPasswordHash())) throw invalidCredentials();
        if (!user.isVerified()) throw new ApiException(403, "email_not_verified", "Vérifiez votre adresse email avant de vous connecter.");
        var session = sessions.save(new AuthSession(user.getId(), clock.instant()));
        return sessionResult(user, session);
    }
    public AuthService.SessionResult refresh(String token) {
        var stored = findToken(token);
        var initialSession = sessions.findById(stored.getSessionId()).orElseThrow(this::invalidSession);
        // Serialize refresh, logout and password reset on the account, in the same lock order.
        var user = users.lockById(initialSession.getUserId()).orElseThrow(this::invalidSession);
        // Refresh the token/session after acquiring the lock: another request may have consumed it.
        // MySQL REPEATABLE READ requires locking reads to see another transaction's committed rotation.
        entityManager.refresh(stored, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(initialSession, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!initialSession.active(clock.instant()) || !user.isVerified()) throw invalidSession();
        if (stored.isUsed()) { initialSession.revoke(); throw invalidSession(); }
        stored.consume();
        return sessionResult(user, initialSession);
    }
    private final jakarta.persistence.EntityManager entityManager;

    public void logout(String token) {
        if (token == null || token.isBlank() || token.length() > 200) return;
        var stored = refreshTokens.findById(secrets.tokenHash(token)).orElse(null);
        if (stored == null) return;
        var session = sessions.findById(stored.getSessionId()).orElse(null);
        if (session != null) {
            users.lockById(session.getUserId());
            entityManager.refresh(session, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            session.revoke();
        }
    }
    private RefreshToken findToken(String token) {
        if (token == null || token.isBlank() || token.length() > 200) throw invalidSession();
        return refreshTokens.findById(secrets.tokenHash(token)).orElseThrow(this::invalidSession);
    }
    private AuthService.SessionResult sessionResult(UserAccount user, AuthSession session) {
        var refresh = secrets.newToken();
        refreshTokens.save(new RefreshToken(secrets.tokenHash(refresh), session.getId()));
        return new AuthService.SessionResult(
            new LoginResponse(tokens.issue(user.getId(), session.getId()), "Bearer", 900, UserResponse.from(user)),
            refresh, session.getExpiresAt());
    }
    @Transactional(readOnly = true)
    public UserResponse me(UUID id) {
        return UserResponse.from(users.findById(id).orElseThrow(this::invalidSession));
    }
    private static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private static void validatePassword(String password) {
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "invalid_password", "Le mot de passe doit contenir au moins 8 caractères et au plus 72 octets UTF-8.");
    }
    private ApiException invalidCode() { return new InvalidCodeException(); }
    private ApiException invalidCredentials() { return new ApiException(401, "invalid_credentials", "Email ou mot de passe incorrect."); }
    private ApiException invalidSession() { return new InvalidSessionException(); }
}
