package com.analytics.repository;

import com.analytics.config.RedisSchema;
import com.analytics.config.MetricsWindowConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ActiveUsersRepositoryTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private ActiveUsersRepository repository;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        repository = new ActiveUsersRepository(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void prunesExpiredEntriesOnWrite() {
        Instant base = Instant.parse("2024-01-01T00:00:00Z");
        Instant stale = base;
        Instant fresh = base.plusSeconds(MetricsWindowConfig.ACTIVE_USER_WINDOW.toSeconds() + 1);

        repository.recordActivity("stale-user", stale);
        repository.recordActivity("fresh-user", fresh);

        long count = repository.countActiveUsers(MetricsWindowConfig.ACTIVE_USER_WINDOW, fresh);
        assertThat(count).isEqualTo(1);
        assertThat(redisTemplate.opsForZSet().zCard(RedisSchema.KEY_ACTIVE_USERS)).isEqualTo(1);
    }
}
