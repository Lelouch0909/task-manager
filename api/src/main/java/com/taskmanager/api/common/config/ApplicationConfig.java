package com.taskmanager.api.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
}
