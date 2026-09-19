package com.taskmanager.api.notification.service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
public interface LiveUpdateService { SseEmitter subscribe(Jwt jwt); }
