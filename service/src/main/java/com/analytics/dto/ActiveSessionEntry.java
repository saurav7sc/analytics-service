package com.analytics.dto;

public class ActiveSessionEntry {
    private final String userId;
    private final long activeSessions;

    public ActiveSessionEntry(String userId, long activeSessions) {
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
