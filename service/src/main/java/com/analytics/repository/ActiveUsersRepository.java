package com.analytics.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;

@Repository
public class ActiveUsersRepository {

    private static final String ACTIVE_USERS_KEY = "stats:users";

    private final StringRedisTemplate redisTemplate;

    public ActiveUsersRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordActivity(String userId, Instant timestamp) {
        redisTemplate.opsForZSet().add(ACTIVE_USERS_KEY, userId, timestamp.toEpochMilli());
    }

    public long countActiveUsers(Duration window, Instant now) {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        long cutoffExclusive = now.minus(window).toEpochMilli() - 1;
        ops.removeRangeByScore(ACTIVE_USERS_KEY, Double.NEGATIVE_INFINITY, cutoffExclusive);
        Long count = ops.zCard(ACTIVE_USERS_KEY);
        return count != null ? count : 0L;
    }
}
