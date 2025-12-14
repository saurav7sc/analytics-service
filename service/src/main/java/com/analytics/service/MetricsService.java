package com.analytics.service;

import com.analytics.model.PageViewMetric;
import com.analytics.model.SessionMetric;
import com.analytics.repository.ActiveUsersRepository;
import com.analytics.repository.PageViewRepository;
import com.analytics.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);
    private static final Duration ACTIVE_USER_WINDOW = Duration.ofMinutes(5);
    private static final Duration PAGE_VIEW_WINDOW = Duration.ofMinutes(15);

    private final Clock clock;
    private final ActiveUsersRepository activeUsersRepository;
    private final PageViewRepository pageViewRepository;
    private final SessionRepository sessionRepository;

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
        long count = activeUsersRepository.countActiveUsers(ACTIVE_USER_WINDOW, now);
        log.debug("active users count={} windowSeconds={}", count, ACTIVE_USER_WINDOW.toSeconds());
        return count;
    }

    public List<PageViewMetric> topPages(int limit) {
        Instant now = Instant.now(clock);
        List<Map.Entry<String, Long>> entries = pageViewRepository.topPages(PAGE_VIEW_WINDOW, now, limit);
        log.debug("top pages computed size={} windowSeconds={} limit={}", entries.size(), PAGE_VIEW_WINDOW.toSeconds(), limit);
        return entries.stream()
                .map(e -> new PageViewMetric(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<SessionMetric> activeSessionsByUser() {
        Instant now = Instant.now(clock);
        List<SessionMetric> metrics = sessionRepository.activeSessionsByUser(ACTIVE_USER_WINDOW, now)
                .stream()
                .map(e -> new SessionMetric(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        log.debug("active sessions entries={} windowSeconds={}", metrics.size(), ACTIVE_USER_WINDOW.toSeconds());
        return metrics;
    }
}
