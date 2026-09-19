package com.taskmanager.api.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import com.taskmanager.api.common.exception.ApiException;

@Entity @Table(name = "auth_codes") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthCode {
    @Id private UUID id;
    @Column(nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CodePurpose purpose;
    @Column(nullable = false, length = 64) private String codeHash;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int attempts;
    @Column(nullable = false) private boolean consumed;
    @Column(nullable = false) private Instant lastSentAt;
    @Column(nullable = false) private Instant windowStart;
    @Column(nullable = false) private int sends;

    public AuthCode(UUID userId, CodePurpose purpose, String hash, Instant now) {
        id = UUID.randomUUID(); this.userId = userId; this.purpose = purpose;
        windowStart = now; sends = 0;
        issue(hash, now);
    }
    public void reissue(String hash, Instant now) {
        if (now.isBefore(lastSentAt.plusSeconds(60)))
            throw new ApiException(429, "code_cooldown", "Attendez 60 secondes entre deux envois.");
        if (!now.isBefore(windowStart.plus(Duration.ofHours(1)))) { windowStart = now; sends = 0; }
        if (sends >= 5) throw new ApiException(429, "code_send_limit", "Limite horaire d’envoi atteinte.");
        issue(hash, now);
    }
    private void issue(String hash, Instant now) {
        codeHash = hash; expiresAt = now.plusSeconds(600); lastSentAt = now;
        attempts = 0; consumed = false; sends++;
    }
    public boolean consume(String candidateHash, Instant now) {
        if (consumed || attempts >= 5 || !now.isBefore(expiresAt)) return false;
        attempts++;
        if (!MessageDigest.isEqual(codeHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8))) return false;
        consumed = true;
        return true;
    }
}
