package com.taskmanager.api.auth.repository;
import com.taskmanager.api.auth.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {
    Optional<AuthCode> findByUserIdAndPurpose(UUID userId, CodePurpose purpose);
}
