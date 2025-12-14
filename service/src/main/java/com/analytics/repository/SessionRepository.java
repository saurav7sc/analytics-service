package com.analytics.repository;

import com.analytics.config.MetricsWindowConfig;
import com.analytics.config.RedisSchema;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class SessionRepository {

    private final StringRedisTemplate redisTemplate;

    public SessionRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordSession(String userId, String sessionId, Instant timestamp) {
        String key = sessionKey(userId);
        redisTemplate.opsForZSet().add(key, sessionId, timestamp.toEpochMilli());
        redisTemplate.expire(key, MetricsWindowConfig.SESSION_RETENTION);
    }

    public List<Map.Entry<String, Long>> activeSessionsByUser(Duration window, Instant now) {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        long cutoffExclusive = now.minus(window).toEpochMilli() - 1;

        Map<String, Long> countsByUser = new java.util.HashMap<>();
        scanKeys(sessionKey("*"), key -> {
            long count = cleanupAndCount(ops, key, cutoffExclusive);
            if (count > 0) {
                countsByUser.put(extractUserId(key), count);
            }
        });

        return countsByUser.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private long cleanupAndCount(ZSetOperations<String, String> ops, String key, long cutoffExclusive) {
        ops.removeRangeByScore(key, Double.NEGATIVE_INFINITY, cutoffExclusive);
        Long count = ops.zCard(key);
        return count != null ? count : 0L;
    }

    private String sessionKey(String userId) {
        return RedisSchema.KEY_SESSIONS_PREFIX + userId;
    }

    private String extractUserId(String key) {
        return key.substring(RedisSchema.KEY_SESSIONS_PREFIX.length());
    }

    private void scanKeys(String pattern, java.util.function.Consumer<String> consumer) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
        Cursor<String> scan = redisTemplate.scan(options);
        try {
            while (scan.hasNext()) {
                consumer.accept(scan.next());
            }
        } finally {
            try {
                scan.close();
            } catch (Exception ignored) {
            }
        }
    }
}
