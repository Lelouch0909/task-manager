package com.taskmanager.api.tasks.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import com.taskmanager.api.common.exception.ApiException;

@Entity @Table(name = "tasks") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {
    @Id private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 5000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskStatus status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    public Task(UUID ownerId, String title, String description, TaskStatus status, Instant now) {
        id = UUID.randomUUID(); this.ownerId = ownerId; createdAt = now;
        update(title, description, status == null ? TaskStatus.TODO : status, now);
    }
    public void update(String title, String description, TaskStatus status, Instant now) {
        if (title == null || title.isBlank() || title.strip().length() > 200)
            throw new ApiException(400, "invalid_title", "Le titre doit contenir de 1 à 200 caractères.");
        if (description != null && description.length() > 5000)
            throw new ApiException(400, "invalid_description", "La description est limitée à 5000 caractères.");
        if (status == null) throw new ApiException(400, "invalid_status", "Le statut est obligatoire.");
        this.title = title.strip(); this.description = description; this.status = status; updatedAt = now;
    }
}
