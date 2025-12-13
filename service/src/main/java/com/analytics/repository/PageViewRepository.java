package com.analytics.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PageViewRepository {
    private static final String PAGE_VIEWS_KEY_PREFIX = "stats:pviews:";
    private static final String KNOWN_URLS_KEY = "stats:known_urls";

    private final StringRedisTemplate redisTemplate;

    public PageViewRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordPageView(String pageUrl, Instant timestamp) {
        String key = pageViewKey(pageUrl);
        redisTemplate.opsForZSet().add(key, uniqueEventId(timestamp), timestamp.toEpochMilli());
        redisTemplate.opsForSet().add(KNOWN_URLS_KEY, pageUrl);
    }

    public List<Map.Entry<String, Long>> topPages(Duration window, Instant now, int limit) {
        Set<String> knownUrls = redisTemplate.opsForSet().members(KNOWN_URLS_KEY);
        if (knownUrls == null || knownUrls.isEmpty()) {
            return List.of();
        }

        List<Map.Entry<String, Long>> counts = new ArrayList<>();
        long cutoffExclusive = now.minus(window).toEpochMilli() - 1;

        for (String url : knownUrls) {
            String key = pageViewKey(url);
            ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
            ops.removeRangeByScore(key, Double.NEGATIVE_INFINITY, cutoffExclusive);
            Long count = ops.zCard(key);
            if (count != null && count > 0) {
                counts.add(Map.entry(url, count));
            }
        }

        return counts.stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String pageViewKey(String pageUrl) {
        return PAGE_VIEWS_KEY_PREFIX + pageUrl;
    }

    private String uniqueEventId(Instant timestamp) {
        return timestamp.toEpochMilli() + ":" + UUID.randomUUID();
    }
}
