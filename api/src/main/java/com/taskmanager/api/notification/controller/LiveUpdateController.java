package com.taskmanager.api.notification.controller;
import com.taskmanager.api.notification.controller.api.LiveUpdateApi;
import com.taskmanager.api.notification.service.LiveUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController @RequiredArgsConstructor
public class LiveUpdateController implements LiveUpdateApi {
    private final LiveUpdateService service;
    public ResponseEntity<SseEmitter> subscribe(Jwt jwt) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("X-Accel-Buffering", "no")
            .body(service.subscribe(jwt));
    }
}
