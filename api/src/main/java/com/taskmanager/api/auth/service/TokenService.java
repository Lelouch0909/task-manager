package com.taskmanager.api.auth.service;
import java.util.UUID;
public interface TokenService { String issue(UUID userId, UUID sessionId); }
