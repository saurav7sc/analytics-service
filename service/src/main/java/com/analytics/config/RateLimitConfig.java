package com.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "analytics.rate-limit")
public class RateLimitConfig {
    /**
     * Maximum burst size (tokens).
     */
    private int capacity = 200;
    /**
     * Token refill rate per second.
     */
    private double refillPerSecond = 3.33;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getRefillPerSecond() {
        return refillPerSecond;
    }

    public void setRefillPerSecond(double refillPerSecond) {
        this.refillPerSecond = refillPerSecond;
    }
}
