package com.taskmanager.api.auth.unit;
import com.taskmanager.api.auth.service.impl.RateLimitServiceImpl;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class RateLimitServiceTest {
    @Test void isolatesKeysAndResetsWindows() {
        var clock = mock(Clock.class);
        var now = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        var service = new RateLimitServiceImpl(clock);
        service.check("ip1", 1);
        assertThatThrownBy(() -> service.check("ip1", 1)).isInstanceOf(ApiException.class);
        service.check("ip2", 1);
        when(clock.instant()).thenReturn(now.plusSeconds(60));
        service.check("ip1", 1);
    }
}
