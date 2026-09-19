package com.taskmanager.api.auth.repository;
import com.taskmanager.api.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {}
