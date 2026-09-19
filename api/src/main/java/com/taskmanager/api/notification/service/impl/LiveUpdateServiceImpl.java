package com.taskmanager.api.notification.service.impl;

import com.taskmanager.api.notification.service.LiveUpdateService;
import com.taskmanager.api.common.exception.ApiException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;

@Service
public class LiveUpdateServiceImpl implements LiveUpdateService {
    public record Sync(boolean tasks, boolean notifications) {}
    private record Connection(UUID id, UUID ownerId, Jwt jwt, SseEmitter emitter) {}
    private final ConcurrentMap<UUID, Connection> connections = new ConcurrentHashMap<>();
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Executor executor;
    private final int perUserLimit;

    public LiveUpdateServiceImpl(JwtDecoder decoder, Clock clock, @Qualifier("sseExecutor") Executor executor,
            @Value("${app.sse.max-per-user:8}") int perUserLimit) {
        this.decoder = decoder; this.clock = clock; this.executor = executor; this.perUserLimit = perUserLimit;
    }
    public synchronized SseEmitter subscribe(Jwt jwt) {
        UUID owner = UUID.fromString(jwt.getSubject());
        if (connections.size() >= 1000 || connections.values().stream().filter(c -> c.ownerId().equals(owner)).count() >= perUserLimit)
            throw new ApiException(429, "stream_limit", "Trop de connexions temps réel ouvertes.");
        long ttl = Duration.between(clock.instant(), jwt.getExpiresAt()).toMillis();
        if (ttl <= 0) throw new ApiException(401, "unauthorized", "Token expiré.");
        var emitter = new SseEmitter(Math.min(ttl, 900_000));
        var connection = new Connection(UUID.randomUUID(), owner, jwt, emitter);
        connections.put(connection.id(), connection);
        emitter.onCompletion(() -> connections.remove(connection.id()));
        emitter.onTimeout(() -> close(connection));
        emitter.onError(ex -> close(connection));
        // The first event forces a REST reload, including all changes missed while disconnected.
        send(connection, new Sync(true, true));
        return emitter;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommitted(LiveUpdate event) {
        connections.values().stream().filter(c -> c.ownerId().equals(event.ownerId()))
            .forEach(c -> enqueue(c, new Sync(event.tasks(), event.notifications())));
    }
    @Scheduled(fixedDelayString = "${app.sse.heartbeat-ms:15000}")
    public void heartbeat() {
        connections.values().forEach(c -> enqueue(c, null));
    }
    private void enqueue(Connection connection, Sync update) {
        try { executor.execute(() -> send(connection, update)); }
        catch (RejectedExecutionException ex) { close(connection); }
    }
    private void send(Connection connection, Sync update) {
        synchronized (connection) {
            if (!connections.containsKey(connection.id())) return;
            try {
                // Every data write and heartbeat revalidates expiry AND revocation.
                decoder.decode(connection.jwt().getTokenValue());
                if (update == null) connection.emitter().send(SseEmitter.event().comment("keepalive"));
                else connection.emitter().send(SseEmitter.event().name("sync").data(update));
            } catch (JwtException | IOException | IllegalStateException ex) {
                close(connection);
            }
        }
    }
    private void close(Connection connection) {
        if (connections.remove(connection.id()) != null) connection.emitter().complete();
    }
    @PreDestroy void shutdown() { connections.values().forEach(this::close); }
}
