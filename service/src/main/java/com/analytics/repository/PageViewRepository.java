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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PageViewRepository {

    private final StringRedisTemplate redisTemplate;

    public PageViewRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordPageView(String pageUrl, Instant timestamp) {
        String key = pageViewKey(pageUrl);
        redisTemplate.opsForZSet().add(key, uniqueEventId(timestamp), timestamp.toEpochMilli());
        redisTemplate.expire(key, MetricsWindowConfig.PAGE_VIEW_RETENTION);
    }

    public List<Map.Entry<String, Long>> topPages(Duration window, Instant now, int limit) {
        List<Map.Entry<String, Long>> counts = new ArrayList<>();
        long cutoffExclusive = now.minus(window).toEpochMilli() - 1;

        scanKeys(pageViewKey("*"), key -> {
            ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
            ops.removeRangeByScore(key, Double.NEGATIVE_INFINITY, cutoffExclusive);
            Long count = ops.zCard(key);
            if (count != null && count > 0) {
                counts.add(Map.entry(extractPageUrl(key), count));
            }
        });

        return counts.stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String pageViewKey(String pageUrl) {
        return RedisSchema.KEY_PAGE_VIEWS_PREFIX + pageUrl;
    }

    private String extractPageUrl(String key) {
        return key.substring(RedisSchema.KEY_PAGE_VIEWS_PREFIX.length());
    }

    private String uniqueEventId(Instant timestamp) {
        return timestamp.toEpochMilli() + ":" + UUID.randomUUID();
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
