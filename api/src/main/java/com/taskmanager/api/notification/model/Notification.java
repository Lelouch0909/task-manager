package com.taskmanager.api.notification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "notifications") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private UUID taskId;
    @Column(nullable = false, length = 30) private String kind;
    @Column(nullable = false, length = 300) private String message;
    @Column(nullable = false) private Instant createdAt;
    private Instant readAt;

    public Notification(UUID ownerId, UUID taskId, String kind, String message, Instant now) {
        this.id = UUID.randomUUID(); this.ownerId = ownerId; this.taskId = taskId;
        this.kind = kind; this.message = message; this.createdAt = now;
    }
    public boolean setRead(boolean read, Instant now) {
        if (read == (readAt != null)) return false;
        readAt = read ? now : null;
        return true;
    }
}
