package com.analytics.service;

import com.analytics.config.RateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimiterService {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String RATE_LIMIT_LUA = """
            local key = KEYS[1]
            local now_ms = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local refill_per_sec = tonumber(ARGV[3])

            local state = redis.call('HMGET', key, 'tokens', 'last_ms')
            local tokens = tonumber(state[1])
            local last_ms = tonumber(state[2])

            if tokens == nil then
              tokens = capacity
              last_ms = now_ms
            end

            local elapsed_ms = now_ms - last_ms
            if elapsed_ms > 0 then
              local refill = (elapsed_ms * refill_per_sec) / 1000.0
              tokens = math.min(capacity, tokens + refill)
              last_ms = now_ms
            end

            -- limit precision drift; keeps decimals for fractional refill
            tokens = math.floor(tokens * 1000 + 0.5) / 1000

            local allowed = 0
            local retry_after_ms = 0

            if tokens >= 1 then
              allowed = 1
              tokens = tokens - 1
              tokens = math.floor(tokens * 1000 + 0.5) / 1000
            else
              local deficit = 1 - tokens
              retry_after_ms = math.ceil((deficit / refill_per_sec) * 1000)
            end

            redis.call('HMSET', key, 'tokens', tokens, 'last_ms', last_ms)
            local ttl_ms = math.ceil((capacity / refill_per_sec) * 1000) + 2000
            redis.call('PEXPIRE', key, ttl_ms)

            return {allowed, math.floor(tokens + 0.0001), retry_after_ms}
            """;
    private static final Duration REDIS_RETRY_DELAY = Duration.ofSeconds(10);

    private final RateLimitConfig config;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> rateLimitScript;
    private final AtomicBoolean redisHealthy = new AtomicBoolean(true);
    private final AtomicLong nextRedisRetryEpochMillis = new AtomicLong(0);

    public RateLimiterService(RateLimitConfig config,
                              ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.config = config;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.rateLimitScript = redisTemplate == null ? null : new DefaultRedisScript<>(RATE_LIMIT_LUA, List.class);
    }

    public boolean allowRequest(String key, Instant now) {
        if (redisTemplate == null || rateLimitScript == null) {
            log.error("Redis is not configured; rejecting request to avoid unbounded traffic");
            return false;
        }
        if (config.getCapacity() <= 0 || config.getRefillPerSecond() <= 0) {
            log.error("Rate limit configuration is invalid (capacity={}, refillPerSecond={}); rejecting request",
                    config.getCapacity(), config.getRefillPerSecond());
            return false;
        }
        if (!redisHealthy.get() && !shouldRetryRedis(now)) {
            return false;
        }
        try {
            boolean allowed = allowWithRedis(key, now);
            if (!redisHealthy.getAndSet(true)) {
                log.info("Redis connection restored; resuming Redis-backed rate limiting");
            }
            return allowed;
        } catch (DataAccessException ex) {
            scheduleRedisRetry(now);
            if (redisHealthy.compareAndSet(true, false)) {
                log.warn("Redis unavailable; denying requests until Redis is restored: {}", ex.getMessage());
            } else {
                log.debug("Redis still unavailable: {}", ex.getMessage());
            }
            return false;
        }
    }

    private boolean allowWithRedis(String key, Instant now) {
        long nowMillis = now.toEpochMilli();
        List<String> keys = Collections.singletonList("rate:" + key);
        List<?> result = redisTemplate.execute(
                rateLimitScript,
                keys,
                String.valueOf(nowMillis),
                String.valueOf(config.getCapacity()),
                String.valueOf(config.getRefillPerSecond())
        );
        if (result == null || result.isEmpty()) {
            return false;
        }
        Object allowedFlag = result.get(0);
        return allowedFlag instanceof Number && ((Number) allowedFlag).longValue() == 1L;
    }

    private void scheduleRedisRetry(Instant now) {
        nextRedisRetryEpochMillis.set(now.toEpochMilli() + REDIS_RETRY_DELAY.toMillis());
    }

    private boolean shouldRetryRedis(Instant now) {
        return now.toEpochMilli() >= nextRedisRetryEpochMillis.get();
    }
}
