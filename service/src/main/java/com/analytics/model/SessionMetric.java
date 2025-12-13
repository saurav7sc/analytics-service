package com.analytics.model;

public class SessionMetric {
    private final String userId;
    private final long activeSessions;

    public SessionMetric(String userId, long activeSessions) {
        this.userId = userId;
        this.activeSessions = activeSessions;
    }

    public String getUserId() {
        return userId;
    }

    public long getActiveSessions() {
        return activeSessions;
    }
}
