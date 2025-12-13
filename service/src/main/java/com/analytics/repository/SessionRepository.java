package com.analytics.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class SessionRepository {

    private static final String ACTIVE_SESSIONS_KEY = "stats:sessions";

    private final StringRedisTemplate redisTemplate;

    public SessionRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordSession(String userId, String sessionId, Instant timestamp) {
        String member = userId + ":" + sessionId;
        redisTemplate.opsForZSet().add(ACTIVE_SESSIONS_KEY, member, timestamp.toEpochMilli());
    }

    public List<Map.Entry<String, Long>> activeSessionsByUser(Duration window, Instant now) {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        long cutoffExclusive = now.minus(window).toEpochMilli() - 1;
        ops.removeRangeByScore(ACTIVE_SESSIONS_KEY, Double.NEGATIVE_INFINITY, cutoffExclusive);

        Set<String> members = ops.range(ACTIVE_SESSIONS_KEY, 0, -1);
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        Map<String, Long> countsByUser = members.stream()
                .map(this::parseUserId)
                .filter(user -> !user.isEmpty())
                .collect(Collectors.groupingBy(user -> user, Collectors.counting()));

        return countsByUser.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private String parseUserId(String member) {
        int separatorIndex = member.indexOf(':');
        if (separatorIndex <= 0) {
            return "";
        }
        return member.substring(0, separatorIndex);
    }
}
