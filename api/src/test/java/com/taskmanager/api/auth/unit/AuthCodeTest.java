package com.taskmanager.api.auth.unit;
import com.taskmanager.api.auth.model.*;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AuthCodeTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private AuthCode code() { return new AuthCode(UUID.randomUUID(), CodePurpose.VERIFY_EMAIL, "hash", now); }
    @Test void codeIsSingleUse() {
        var code = code();
        assertThat(code.consume("hash", now)).isTrue();
        assertThat(code.consume("hash", now)).isFalse();
    }
    @Test void expiresAtExactDeadline() {
        assertThat(code().consume("hash", now.plusSeconds(600))).isFalse();
    }
    @Test void locksAfterFiveWrongAttempts() {
        var code = code();
        for (int i = 0; i < 5; i++) assertThat(code.consume("wrong", now)).isFalse();
        assertThat(code.consume("hash", now)).isFalse();
    }
    @Test void enforcesCooldownAndHourlyLimit() {
        var code = code();
        assertThatThrownBy(() -> code.reissue("new", now.plusSeconds(59))).isInstanceOf(ApiException.class);
        for (int i = 1; i <= 4; i++) code.reissue("new", now.plusSeconds(i * 60));
        assertThatThrownBy(() -> code.reissue("new", now.plusSeconds(300))).isInstanceOf(ApiException.class);
        code.reissue("last", now.plusSeconds(3600));
        assertThat(code.consume("last", now.plusSeconds(3600))).isTrue();
    }
    @Test void reissueInvalidatesPreviousCodeAndResetsAttempts() {
        var code = code();
        code.consume("wrong", now);
        code.reissue("next", now.plusSeconds(60));
        assertThat(code.getAttempts()).isZero();
        assertThat(code.consume("hash", now.plusSeconds(60))).isFalse();
        assertThat(code.consume("next", now.plusSeconds(60))).isTrue();
    }
}
