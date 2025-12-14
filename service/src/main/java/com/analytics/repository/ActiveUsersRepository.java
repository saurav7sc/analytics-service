package com.analytics.repository;

import com.analytics.config.MetricsWindowConfig;
import com.analytics.config.RedisSchema;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;

@Repository
public class ActiveUsersRepository {
    private final StringRedisTemplate redisTemplate;

    public ActiveUsersRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordActivity(String userId, Instant timestamp) {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        ops.add(RedisSchema.KEY_ACTIVE_USERS, userId, timestamp.toEpochMilli());
        long cutoffExclusive = timestamp.minus(MetricsWindowConfig.ACTIVE_USER_RETENTION).toEpochMilli() - 1;
        ops.removeRangeByScore(RedisSchema.KEY_ACTIVE_USERS, Double.NEGATIVE_INFINITY, cutoffExclusive);
    }

    public long countActiveUsers(Duration window, Instant now) {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        long cutoffExclusive = now.minus(window).toEpochMilli() - 1;
        ops.removeRangeByScore(RedisSchema.KEY_ACTIVE_USERS, Double.NEGATIVE_INFINITY, cutoffExclusive);
        Long count = ops.zCard(RedisSchema.KEY_ACTIVE_USERS);
        return count != null ? count : 0L;
    }
}
