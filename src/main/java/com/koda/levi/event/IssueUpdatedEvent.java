package com.koda.levi.event;

import com.koda.levi.entity.Issue;
import org.springframework.context.ApplicationEvent;

public class IssueUpdatedEvent extends ApplicationEvent {

    private final Issue issue;
    private final String action;
    private final Long projectId;

    public IssueUpdatedEvent(Object source, Issue issue, String action, Long projectId) {
        super(source);
        this.issue = issue;
        this.action = action;
        this.projectId = projectId;
    }

    public Issue getIssue() {
        return issue;
    }

    public String getAction() {
        return action;
    }

    public Long getProjectId() {
        return projectId;
    }
}
