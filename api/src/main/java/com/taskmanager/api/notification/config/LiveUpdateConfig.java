package com.taskmanager.api.notification.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration @EnableScheduling
public class LiveUpdateConfig {
    @Bean(name = "sseExecutor")
    ThreadPoolTaskExecutor sseExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("sse-");
        executor.setDaemon(true);
        return executor;
    }
}
