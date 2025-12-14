package com.analytics.config;

import java.time.Duration;

public final class MetricsWindowConfig {
    public static final Duration ACTIVE_USER_WINDOW = Duration.ofMinutes(5);
    public static final Duration PAGE_VIEW_WINDOW = Duration.ofMinutes(15);
    public static final Duration SESSION_WINDOW = Duration.ofMinutes(5);

    private MetricsWindowConfig() {
    }
}
