package com.analytics.service;

import com.analytics.config.RateLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    @Test
    void deniesWhenRedisNotConfigured() {
        RateLimiterService service = new RateLimiterService(defaultConfig(), provider(null));

        assertThat(service.allowRequest("client", Instant.now())).isFalse();
    }

    @Test
    void allowsWhenRedisAcceptsRequest() {
        StubStringRedisTemplate redisTemplate = new StubStringRedisTemplate()
                .enqueue(List.of(1L, 199L, 0L));
        RateLimiterService service = new RateLimiterService(defaultConfig(), provider(redisTemplate));

        boolean allowed = service.allowRequest("client", Instant.parse("2024-01-01T00:00:00Z"));

        assertThat(allowed).isTrue();
        assertThat(redisTemplate.executions).isEqualTo(1);
    }

    @Test
    void deniesWhileRedisDownAndRetriesAfterBackoff() {
        StubStringRedisTemplate redisTemplate = new StubStringRedisTemplate()
                .enqueue(new DataAccessResourceFailureException("redis down"))
                .enqueue(List.of(1L, 198L, 0L));
        RateLimiterService service = new RateLimiterService(defaultConfig(), provider(redisTemplate));

        Instant base = Instant.parse("2024-01-01T00:00:00Z");
        boolean first = service.allowRequest("client", base);
        boolean second = service.allowRequest("client", base.plusSeconds(5));
        boolean third = service.allowRequest("client", base.plusSeconds(11));

        assertThat(first).isFalse();
        assertThat(second).isFalse();
        assertThat(third).isTrue();
        assertThat(redisTemplate.executions).isEqualTo(2);
    }

    @Test
    void deniesWhenBucketEmpty() {
        StubStringRedisTemplate redisTemplate = new StubStringRedisTemplate()
                .enqueue(List.of(0L, 0L, 500L));
        RateLimiterService service = new RateLimiterService(defaultConfig(), provider(redisTemplate));

        boolean allowed = service.allowRequest("client", Instant.parse("2024-01-01T00:00:00Z"));

        assertThat(allowed).isFalse();
    }

    private RateLimitConfig defaultConfig() {
        RateLimitConfig config = new RateLimitConfig();
        config.setCapacity(200);
        config.setRefillPerSecond(3.33);
        return config;
    }

    private ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate template) {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject(Object... args) {
                return template;
            }

            @Override
            public StringRedisTemplate getObject() {
                return template;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return template;
            }

            @Override
            public StringRedisTemplate getIfAvailable(Supplier<StringRedisTemplate> defaultSupplier) {
                return template != null ? template : defaultSupplier.get();
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return template;
            }

            @Override
            public StringRedisTemplate getIfUnique(Supplier<StringRedisTemplate> defaultSupplier) {
                return template != null ? template : defaultSupplier.get();
            }

            @Override
            public void forEach(Consumer<? super StringRedisTemplate> action) {
                if (template != null) {
                    action.accept(template);
                }
            }
        };
    }

    private static class StubStringRedisTemplate extends StringRedisTemplate {
        private final Queue<Object> outcomes = new ArrayDeque<>();
        private int executions = 0;

        StubStringRedisTemplate enqueue(Object outcome) {
            outcomes.add(outcome);
            return this;
        }

        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) throws DataAccessException {
            executions++;
            Object outcome = outcomes.poll();
            if (outcome instanceof RuntimeException ex) {
                throw ex;
            }
            return (T) Objects.requireNonNull(outcome, "Stub outcome must be enqueued");
        }
    }
}
