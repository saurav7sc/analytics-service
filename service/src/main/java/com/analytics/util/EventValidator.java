package com.analytics.util;

import com.analytics.dto.EventRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class EventValidator {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private EventValidator() {
    }

    public static void validate(EventRequest request) {
        Validator validator = FACTORY.getValidator();
        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(message);
        }
    }
}
