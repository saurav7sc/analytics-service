package com.analytics.dto;

import java.util.List;

public class PageViewsResponse {
    private final List<PageViewEntry> topPages;
    private final long windowSeconds;

    public PageViewsResponse(List<PageViewEntry> topPages, long windowSeconds) {
        this.topPages = topPages;
        this.windowSeconds = windowSeconds;
    }

    public List<PageViewEntry> getTopPages() {
        return topPages;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
