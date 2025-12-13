package com.analytics.dto;

public class PageViewEntry {
    private final String pageUrl;
    private final long count;

    public PageViewEntry(String pageUrl, long count) {
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
