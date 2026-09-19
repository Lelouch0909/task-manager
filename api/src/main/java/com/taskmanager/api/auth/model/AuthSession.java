package com.taskmanager.api.auth.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "auth_sessions") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession {
    @Id private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;
    public AuthSession(UUID userId, Instant now) {
        id = UUID.randomUUID(); this.userId = userId; expiresAt = now.plusSeconds(7 * 24 * 3600);
    }
    public void revoke() { revoked = true; }
    public boolean active(Instant now) { return !revoked && now.isBefore(expiresAt); }
}
