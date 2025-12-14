package com.analytics.config;

public final class RedisSchema {
    public static final String KEY_PAGE_VIEWS_PREFIX = "stats:pviews:";
    public static final String KEY_SESSIONS_PREFIX = "stats:sessions:";
    public static final String KEY_ACTIVE_USERS = "stats:users";

    private RedisSchema() {
    }
}
