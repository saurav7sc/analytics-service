package com.analytics.service;

import com.analytics.dto.EventRequest;
import com.analytics.util.EventValidator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ValidationService {
    private final Clock clock;

    public ValidationService(Clock clock) {
        this.clock = clock;
    }

    public ValidationService() {
        this(Clock.systemUTC());
    }

    public void validate(EventRequest request) {
        EventValidator.validate(request);
        Instant now = Instant.now(clock);
        if (request.getTimestamp() == null) {
            throw new IllegalArgumentException("timestamp required");
        }
        if (request.getTimestamp().isAfter(now.plusSeconds(5))) {
            throw new IllegalArgumentException("timestamp cannot be in the far future");
        }
    }
}
