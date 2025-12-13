package com.analytics.service;

import com.analytics.dto.EventRequest;
import com.analytics.model.Event;
import com.analytics.processor.EventProcessor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final ValidationService validationService;
    private final EventProcessor eventProcessor;

    public EventIngestionService(ValidationService validationService, EventProcessor eventProcessor) {
        this.validationService = validationService;
        this.eventProcessor = eventProcessor;
    }

    public void ingest(EventRequest request) {
        validationService.validate(request);
        Event event = new Event(
                request.getTimestamp(),
                request.getUserId(),
                request.getEventType(),
                request.getPageUrl(),
                request.getSessionId()
        );
        eventProcessor.process(event);
        log.debug("event accepted: type={} page={} user={} session={}",
                request.getEventType(),
                request.getPageUrl(),
                request.getUserId(),
                request.getSessionId());
    }
}
