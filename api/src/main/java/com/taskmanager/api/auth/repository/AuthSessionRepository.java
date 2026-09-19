package com.taskmanager.api.auth.repository;
import com.taskmanager.api.auth.model.AuthSession;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    List<AuthSession> findByUserId(UUID userId);
}
