package com.taskmanager.api.auth.service.impl;
import com.taskmanager.api.auth.service.RateLimitService;
import com.taskmanager.api.common.exception.ApiException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    private record Window(Instant until, int count) {}
    public synchronized void check(String key, int limit) {
        var now = clock.instant();
        windows.entrySet().removeIf(e -> !now.isBefore(e.getValue().until()));
        var current = windows.get(key);
        if (current == null && windows.size() >= 10000) throw limited();
        if (current != null && current.count() >= limit) throw limited();
        windows.put(key, current == null ? new Window(now.plusSeconds(60), 1)
            : new Window(current.until(), current.count() + 1));
    }
    private ApiException limited() { return new ApiException(429, "rate_limited", "Trop de tentatives. Réessayez dans une minute."); }
}
