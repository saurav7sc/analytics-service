package com.analytics.service;

import com.analytics.model.Event;
import com.analytics.processor.EventProcessor;
import com.analytics.repository.ActiveUsersRepository;
import com.analytics.repository.PageViewRepository;
import com.analytics.repository.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers
class MetricsServiceTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private MutableClock clock;
    private MetricsService metricsService;
    private EventProcessor processor;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        // ensure isolation between tests
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        ActiveUsersRepository activeUsersRepository = new ActiveUsersRepository(redisTemplate);
        SessionRepository sessionRepository = new SessionRepository(redisTemplate);
        PageViewRepository pageViewRepository = new PageViewRepository(redisTemplate);
        processor = new EventProcessor(activeUsersRepository, sessionRepository, pageViewRepository);
        metricsService = new MetricsService(clock, activeUsersRepository, pageViewRepository, sessionRepository);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void countsActiveUsersWithinFiveMinutes() {
        ingest("usr1", "sess1", "/a");
        ingest("usr2", "sess2", "/b");
        assertThat(metricsService.activeUsersCount()).isEqualTo(2);

        clock.advanceSeconds(301);
        assertThat(metricsService.activeUsersCount()).isZero();
    }

    @Test
    void aggregatesPageViewsInLastFifteenMinutes() {
        ingest("usr1", "sess1", "/a");
        ingest("usr1", "sess1", "/a");
        ingest("usr1", "sess1", "/b");

        assertThat(metricsService.topPages(5))
                .extracting("pageUrl", "count")
                .containsExactly(
                        tuple("/a", 2L),
                        tuple("/b", 1L)
                );

        clock.advanceSeconds(901);
        assertThat(metricsService.topPages(5)).isEmpty();
    }

    @Test
    void tracksSessionsPerUser() {
        ingest("usr1", "sess1", "/a");
        ingest("usr1", "sess2", "/b");
        ingest("usr2", "sess9", "/a");

        assertThat(metricsService.activeSessionsByUser())
                .extracting("userId", "activeSessions")
                .contains(
                        tuple("usr1", 2L),
                        tuple("usr2", 1L)
                );

        clock.advanceSeconds(301);
        assertThat(metricsService.activeSessionsByUser()).isEmpty();
    }

    private void ingest(String user, String session, String page) {
        processor.process(new Event(clock.instant(), user, "page_view", page, session));
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
