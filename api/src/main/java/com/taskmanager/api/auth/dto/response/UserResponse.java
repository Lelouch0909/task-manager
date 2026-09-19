package com.taskmanager.api.auth.dto.response;
import com.taskmanager.api.auth.model.UserAccount;
import java.util.UUID;
import java.time.Instant;
public record UserResponse(UUID id, String displayName, String email, boolean emailVerified, Instant createdAt) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.isVerified(), user.getCreatedAt());
    }
}
