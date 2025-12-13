package com.analytics.model;

import java.time.Instant;
import java.util.Objects;

public class Event {
    private final Instant timestamp;
    private final String userId;
    private final String eventType;
    private final String pageUrl;
    private final String sessionId;

    public Event(Instant timestamp, String userId, String eventType, String pageUrl, String sessionId) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.pageUrl = Objects.requireNonNull(pageUrl, "pageUrl");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getSessionId() {
        return sessionId;
    }
}
