package com.analytics.model;

public class PageViewMetric {
    private final String pageUrl;
    private final long count;

    public PageViewMetric(String pageUrl, long count) {
        this.pageUrl = pageUrl;
        this.count = count;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public long getCount() {
        return count;
    }
}
