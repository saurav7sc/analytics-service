package com.analytics.processor;

import com.analytics.model.Event;
import com.analytics.repository.ActiveUsersRepository;
import com.analytics.repository.PageViewRepository;
import com.analytics.repository.SessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EventProcessorTest {

    @Test
    void ignoresNonPageViewEvents() {
        CountingActiveUsers activeUsers = new CountingActiveUsers();
        CountingSessions sessions = new CountingSessions();
        CountingPageViews pageViews = new CountingPageViews();

        EventProcessor processor = new EventProcessor(activeUsers, sessions, pageViews);
        processor.process(new Event(Instant.now(), "user-1", "login", "/a", "sess-1"));

        assertThat(activeUsers.addCount.get()).isZero();
        assertThat(sessions.addCount.get()).isZero();
        assertThat(pageViews.addCount.get()).isZero();
    }

    @Test
    void processesPageViewEvents() {
        CountingActiveUsers activeUsers = new CountingActiveUsers();
        CountingSessions sessions = new CountingSessions();
        CountingPageViews pageViews = new CountingPageViews();

        EventProcessor processor = new EventProcessor(activeUsers, sessions, pageViews);
        processor.process(new Event(Instant.now(), "user-1", "page_view", "/a", "sess-1"));

        assertThat(activeUsers.addCount.get()).isEqualTo(1);
        assertThat(sessions.addCount.get()).isEqualTo(1);
        assertThat(pageViews.addCount.get()).isEqualTo(1);
    }

    private static class CountingActiveUsers extends ActiveUsersRepository {
        private final AtomicInteger addCount = new AtomicInteger();

        CountingActiveUsers() {
            super(null);
        }

        @Override
        public void recordActivity(String userId, Instant timestamp) {
            addCount.incrementAndGet();
        }
    }

    private static class CountingSessions extends SessionRepository {
        private final AtomicInteger addCount = new AtomicInteger();

        CountingSessions() {
            super(null);
        }

        @Override
        public void recordSession(String userId, String sessionId, Instant timestamp) {
            addCount.incrementAndGet();
        }
    }

    private static class CountingPageViews extends PageViewRepository {
        private final AtomicInteger addCount = new AtomicInteger();

        CountingPageViews() {
            super(null);
        }

        @Override
        public void recordPageView(String pageUrl, Instant timestamp) {
            addCount.incrementAndGet();
        }
    }
}
