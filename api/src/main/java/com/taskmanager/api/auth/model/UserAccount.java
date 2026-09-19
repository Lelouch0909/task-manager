package com.taskmanager.api.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "user_accounts") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {
    @Id private UUID id;
    @Column(nullable = false, length = 100) private String displayName;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false) private boolean verified;
    @Column(nullable = false) private Instant createdAt;

    public UserAccount(String displayName, String email, String passwordHash, Instant now) {
        this.id = UUID.randomUUID();
        this.displayName = displayName.strip();
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = now;
    }
    public void verify() { verified = true; }
    public void changePassword(String hash) { passwordHash = hash; }
}
