package com.koda.levi.listener;

import com.koda.levi.event.IssueUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @EventListener
    public void handleIssueUpdatedEvent(IssueUpdatedEvent event) {
        logger.info("Processing IssueUpdatedEvent asynchronously for issue: {}, action: {}", 
                event.getIssue().getId(), event.getAction());

        // Push to WebSocket clients
        String destination = "/topic/projects/" + event.getProjectId() + "/issues";
        messagingTemplate.convertAndSend(destination, event.getIssue());

        // Mock SMTP sending
        if (event.getIssue().getAssignedTo() != null) {
            logger.info("MOCK EMAIL: Sending notification to {} that issue {} was {}", 
                    event.getIssue().getAssignedTo(), event.getIssue().getId(), event.getAction());
        }
    }
}
