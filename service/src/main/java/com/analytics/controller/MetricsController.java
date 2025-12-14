package com.analytics.controller;

import com.analytics.dto.ActiveSessionEntry;
import com.analytics.dto.ActiveSessionsResponse;
import com.analytics.dto.ActiveUsersResponse;
import com.analytics.dto.CombinedMetricsResponse;
import com.analytics.dto.PageViewEntry;
import com.analytics.dto.PageViewsResponse;
import com.analytics.model.SessionMetric;
import com.analytics.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private static final long ACTIVE_USER_WINDOW_SECONDS = 300;
    private static final long PAGE_VIEW_WINDOW_SECONDS = 900;
    private static final long SESSION_WINDOW_SECONDS = 300;

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/active-users")
    public ActiveUsersResponse activeUsers() {
        return new ActiveUsersResponse(metricsService.activeUsersCount(), ACTIVE_USER_WINDOW_SECONDS);
    }

    @GetMapping("/page-views")
    public PageViewsResponse topPages() {
        List<PageViewEntry> entries = metricsService.topPages(5)
                .stream()
                .map(m -> new PageViewEntry(m.getPageUrl(), m.getCount()))
                .collect(Collectors.toList());
        return new PageViewsResponse(entries, PAGE_VIEW_WINDOW_SECONDS);
    }

    @GetMapping("/active-sessions")
    public ActiveSessionsResponse activeSessions() {
        List<SessionMetric> metrics = metricsService.activeSessionsByUser();
        List<ActiveSessionEntry> entries = metrics.stream()
                .map(m -> new ActiveSessionEntry(m.getUserId(), m.getActiveSessions()))
                .collect(Collectors.toList());
        return new ActiveSessionsResponse(entries, SESSION_WINDOW_SECONDS);
    }

    @GetMapping("/summary")
    public CombinedMetricsResponse summary() {
        List<PageViewEntry> pages = metricsService.topPages(5)
                .stream()
                .map(m -> new PageViewEntry(m.getPageUrl(), m.getCount()))
                .collect(Collectors.toList());
        List<ActiveSessionEntry> sessions = metricsService.activeSessionsByUser()
                .stream()
                .map(m -> new ActiveSessionEntry(m.getUserId(), m.getActiveSessions()))
                .collect(Collectors.toList());

        return new CombinedMetricsResponse(
                metricsService.activeUsersCount(),
                ACTIVE_USER_WINDOW_SECONDS,
                pages,
                PAGE_VIEW_WINDOW_SECONDS,
                sessions,
                SESSION_WINDOW_SECONDS
        );
    }
}
