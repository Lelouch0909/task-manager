package com.taskmanager.api.auth.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "refresh_tokens") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id @Column(length = 64) private String tokenHash;
    @Column(nullable = false) private UUID sessionId;
    @Column(nullable = false) private boolean used;
    public RefreshToken(String hash, UUID sessionId) { tokenHash = hash; this.sessionId = sessionId; }
    public void consume() { used = true; }
}
