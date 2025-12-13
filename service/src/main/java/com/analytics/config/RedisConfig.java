package com.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RedisConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
