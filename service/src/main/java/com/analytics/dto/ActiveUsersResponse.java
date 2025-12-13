package com.analytics.dto;

public class ActiveUsersResponse {
    private final long count;
    private final long windowSeconds;

    public ActiveUsersResponse(long count, long windowSeconds) {
        this.count = count;
        this.windowSeconds = windowSeconds;
    }

    public long getCount() {
        return count;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
