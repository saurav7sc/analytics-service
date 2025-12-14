package com.analytics.config;

import java.time.Duration;

public final class MetricsWindowConfig {
    public static final Duration ACTIVE_USER_WINDOW = Duration.ofMinutes(5);
    public static final Duration PAGE_VIEW_WINDOW = Duration.ofMinutes(15);
    public static final Duration SESSION_WINDOW = Duration.ofMinutes(5);
    public static final Duration ACTIVE_USER_RETENTION = Duration.ofMinutes(10);
    public static final Duration PAGE_VIEW_RETENTION = Duration.ofMinutes(30);
    public static final Duration SESSION_RETENTION = Duration.ofMinutes(15);

    private MetricsWindowConfig() {
    }
}
