package com.taskmanager.api.auth.unit;
import com.taskmanager.api.auth.service.impl.AuthStateServiceImpl;
import com.taskmanager.api.auth.service.*;
import com.taskmanager.api.auth.repository.*;
import com.taskmanager.api.auth.model.*;
import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthStateServiceTest {
    @Mock UserAccountRepository users;
    @Mock AuthCodeRepository codes;
    @Mock AuthSessionRepository sessions;
    @Mock RefreshTokenRepository refresh;
    @Mock SecretService secrets;
    @Mock TokenService tokens;
    @Mock PasswordEncoder passwords;
    @Mock EntityManager em;
    AuthStateServiceImpl service;
    final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    @BeforeEach void setup() {
        service = new AuthStateServiceImpl(users, codes, sessions, refresh, secrets, tokens, passwords,
            Clock.fixed(now, ZoneOffset.UTC), em);
    }
    @Test void registrationNormalizesEmailAndHashesPassword() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwords.encode("Password1!")).thenReturn("encoded");
        when(users.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(secrets.newCode()).thenReturn("123456");
        when(secrets.codeHash(any(), any(), any())).thenReturn("hmac");
        when(codes.findByUserIdAndPurpose(any(), any())).thenReturn(Optional.empty());
        var result = service.register(new RegisterRequest(" Alice ", "ALICE@example.com", "Password1!"));
        assertThat(result.user().email()).isEqualTo("alice@example.com");
        assertThat(result.user().displayName()).isEqualTo("Alice");
        assertThat(result.user().emailVerified()).isFalse();
        var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
    }
    @Test void duplicateEmailIsRejected() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(new UserAccount("Alice", "alice@example.com", "hash", now)));
        assertThatThrownBy(() -> service.register(new RegisterRequest("Alice", "alice@example.com", "Password1!")))
            .isInstanceOf(ApiException.class).hasMessageContaining("déjà");
        verify(users, never()).saveAndFlush(any());
    }
    @Test void rejectsMultibytePasswordsOverBcryptLimit() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("Alice", "alice@example.com", "é".repeat(37))))
            .isInstanceOf(ApiException.class);
        verifyNoInteractions(users, passwords);
    }
    @Test void unknownLoginChecksDummyHash() {
        when(users.lockByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("missing@example.com", "Password1!"))).isInstanceOf(ApiException.class);
        verify(passwords).matches(eq("Password1!"), anyString());
    }
    @Test void unverifiedAccountCannotLogin() {
        var user = new UserAccount("Alice", "alice@example.com", "hash", now);
        when(users.lockByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("Password1!", "hash")).thenReturn(true);
        assertThatThrownBy(() -> service.login(new LoginRequest(user.getEmail(), "Password1!")))
            .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(403));
        verifyNoInteractions(sessions);
    }
}
