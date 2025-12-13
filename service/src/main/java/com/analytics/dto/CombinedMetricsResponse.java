package com.analytics.dto;

import java.util.List;

public class CombinedMetricsResponse {
    private final long activeUsers;
    private final long activeUsersWindowSeconds;
    private final List<PageViewEntry> topPages;
    private final long pageViewsWindowSeconds;
    private final List<ActiveSessionEntry> activeSessionsByUser;
    private final long sessionsWindowSeconds;

    public CombinedMetricsResponse(long activeUsers,
                                   long activeUsersWindowSeconds,
                                   List<PageViewEntry> topPages,
                                   long pageViewsWindowSeconds,
                                   List<ActiveSessionEntry> activeSessionsByUser,
                                   long sessionsWindowSeconds) {
        this.activeUsers = activeUsers;
        this.activeUsersWindowSeconds = activeUsersWindowSeconds;
        this.topPages = topPages;
        this.pageViewsWindowSeconds = pageViewsWindowSeconds;
        this.activeSessionsByUser = activeSessionsByUser;
        this.sessionsWindowSeconds = sessionsWindowSeconds;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public long getActiveUsersWindowSeconds() {
        return activeUsersWindowSeconds;
    }

    public List<PageViewEntry> getTopPages() {
        return topPages;
    }

    public long getPageViewsWindowSeconds() {
        return pageViewsWindowSeconds;
    }

    public List<ActiveSessionEntry> getActiveSessionsByUser() {
        return activeSessionsByUser;
    }

    public long getSessionsWindowSeconds() {
        return sessionsWindowSeconds;
    }
}
