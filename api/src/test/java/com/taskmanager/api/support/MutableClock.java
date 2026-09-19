package com.taskmanager.api.support;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
public class MutableClock extends Clock {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now());
    public void advance(Duration duration) { now.updateAndGet(n -> n.plus(duration)); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now.get(); }
}
