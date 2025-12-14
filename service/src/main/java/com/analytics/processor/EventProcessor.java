package com.analytics.processor;

import com.analytics.model.Event;
import com.analytics.repository.ActiveUsersRepository;
import com.analytics.repository.PageViewRepository;
import com.analytics.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventProcessor {

    private final ActiveUsersRepository activeUsersRepository;
    private final SessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;

    @Autowired
    public EventProcessor(ActiveUsersRepository activeUsersRepository,
                          SessionRepository sessionRepository,
                          PageViewRepository pageViewRepository) {
        this.activeUsersRepository = activeUsersRepository;
        this.sessionRepository = sessionRepository;
        this.pageViewRepository = pageViewRepository;
    }

    public void process(Event event) {
        if (!"page_view".equals(event.getEventType())) {
            return;
        }
        activeUsersRepository.recordActivity(event.getUserId(), event.getTimestamp());
        sessionRepository.recordSession(event.getUserId(), event.getSessionId(), event.getTimestamp());
        pageViewRepository.recordPageView(event.getPageUrl(), event.getTimestamp());
    }
}
