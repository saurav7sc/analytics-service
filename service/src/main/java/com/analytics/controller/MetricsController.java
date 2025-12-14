package com.analytics.controller;

import com.analytics.config.MetricsWindowConfig;
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

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/active-users")
    public ActiveUsersResponse activeUsers() {
        return new ActiveUsersResponse(
                metricsService.activeUsersCount(),
                MetricsWindowConfig.ACTIVE_USER_WINDOW.toSeconds()
        );
    }

    @GetMapping("/page-views")
    public PageViewsResponse topPages() {
        List<PageViewEntry> entries = metricsService.topPages(5)
                .stream()
                .map(m -> new PageViewEntry(m.getPageUrl(), m.getCount()))
                .collect(Collectors.toList());
        return new PageViewsResponse(entries, MetricsWindowConfig.PAGE_VIEW_WINDOW.toSeconds());
    }

    @GetMapping("/active-sessions")
    public ActiveSessionsResponse activeSessions() {
        List<SessionMetric> metrics = metricsService.activeSessionsByUser();
        List<ActiveSessionEntry> entries = metrics.stream()
                .map(m -> new ActiveSessionEntry(m.getUserId(), m.getActiveSessions()))
                .collect(Collectors.toList());
        return new ActiveSessionsResponse(entries, MetricsWindowConfig.SESSION_WINDOW.toSeconds());
    }

    @GetMapping("/summary")
    public CombinedMetricsResponse summary() {
        return new CombinedMetricsResponse(
                metricsService.activeUsersCount(),
                MetricsWindowConfig.ACTIVE_USER_WINDOW.toSeconds(),
                getPageViewEntries(),
                MetricsWindowConfig.PAGE_VIEW_WINDOW.toSeconds(),
                getActiveSessionEntries(),
                MetricsWindowConfig.SESSION_WINDOW.toSeconds()
        );
    }

    private List<PageViewEntry> getPageViewEntries() {
        return metricsService.topPages(5)
                .stream()
                .map(m -> new PageViewEntry(m.getPageUrl(), m.getCount()))
                .collect(Collectors.toList());
    }

    private List<ActiveSessionEntry> getActiveSessionEntries() {
        return metricsService.activeSessionsByUser()
                .stream()
                .map(m -> new ActiveSessionEntry(m.getUserId(), m.getActiveSessions()))
                .collect(Collectors.toList());
    }
}
