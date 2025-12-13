package com.analytics.dto;

import java.util.List;

public class ActiveSessionsResponse {
    private final List<ActiveSessionEntry> users;
    private final long windowSeconds;

    public ActiveSessionsResponse(List<ActiveSessionEntry> users, long windowSeconds) {
        this.users = users;
        this.windowSeconds = windowSeconds;
    }

    public List<ActiveSessionEntry> getUsers() {
        return users;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
