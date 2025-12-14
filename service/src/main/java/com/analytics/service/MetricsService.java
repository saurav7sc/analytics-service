package com.analytics.service;

import com.analytics.config.MetricsWindowConfig;
import com.analytics.model.PageViewMetric;
import com.analytics.model.SessionMetric;
import com.analytics.repository.ActiveUsersRepository;
import com.analytics.repository.PageViewRepository;
import com.analytics.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final Clock clock;
    private final ActiveUsersRepository activeUsersRepository;
    private final PageViewRepository pageViewRepository;
    private final SessionRepository sessionRepository;

    private final AtomicLong lastActiveUserCount = new AtomicLong(0);
    private final AtomicReference<List<PageViewMetric>> lastTopPages =
            new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<List<SessionMetric>> lastActiveSessions =
            new AtomicReference<>(Collections.emptyList());

    public MetricsService(Clock clock,
                          ActiveUsersRepository activeUsersRepository,
                          PageViewRepository pageViewRepository,
                          SessionRepository sessionRepository) {
        this.clock = clock;
        this.activeUsersRepository = activeUsersRepository;
        this.pageViewRepository = pageViewRepository;
        this.sessionRepository = sessionRepository;
    }

    public long activeUsersCount() {
        Instant now = Instant.now(clock);
        try {
            long count = activeUsersRepository.countActiveUsers(MetricsWindowConfig.ACTIVE_USER_WINDOW, now);
            lastActiveUserCount.set(count);
            log.debug("active users count={} windowSeconds={}", count, MetricsWindowConfig.ACTIVE_USER_WINDOW.toSeconds());
            return count;
        } catch (Exception ex) {
            log.warn("Redis unavailable, serving stale active user count", ex);
            return lastActiveUserCount.get();
        }
    }

    public List<PageViewMetric> topPages(int limit) {
        Instant now = Instant.now(clock);
        try {
            List<Map.Entry<String, Long>> entries = pageViewRepository.topPages(MetricsWindowConfig.PAGE_VIEW_WINDOW, now, limit);
            List<PageViewMetric> metrics = entries.stream()
                    .map(e -> new PageViewMetric(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            List<PageViewMetric> snapshot = Collections.unmodifiableList(metrics);
            lastTopPages.set(snapshot);
            log.debug("top pages computed size={} windowSeconds={} limit={}", snapshot.size(), MetricsWindowConfig.PAGE_VIEW_WINDOW.toSeconds(), limit);
            return snapshot;
        } catch (Exception ex) {
            log.warn("Redis unavailable, serving stale top pages", ex);
            return lastTopPages.get();
        }
    }

    public List<SessionMetric> activeSessionsByUser() {
        Instant now = Instant.now(clock);
        try {
            List<SessionMetric> metrics = sessionRepository.activeSessionsByUser(MetricsWindowConfig.SESSION_WINDOW, now)
                    .stream()
                    .map(e -> new SessionMetric(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            List<SessionMetric> snapshot = Collections.unmodifiableList(metrics);
            lastActiveSessions.set(snapshot);
            log.debug("active sessions entries={} windowSeconds={}", snapshot.size(), MetricsWindowConfig.SESSION_WINDOW.toSeconds());
            return snapshot;
        } catch (Exception ex) {
            log.warn("Redis unavailable, serving stale active sessions", ex);
            return lastActiveSessions.get();
        }
    }
}
