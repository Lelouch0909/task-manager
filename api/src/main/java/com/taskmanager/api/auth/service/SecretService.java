package com.taskmanager.api.auth.service;
import com.taskmanager.api.auth.model.CodePurpose;
import java.util.UUID;
public interface SecretService {
    String newCode();
    String newToken();
    String tokenHash(String value);
    String codeHash(UUID userId, CodePurpose purpose, String value);
}
