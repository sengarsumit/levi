package com.koda.levi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "issue")
@Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private Integer status;

    @Column(name = "issue_type")
    private String issueType;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    @JsonIgnore
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_state_id")
    @JsonIgnore
    private WorkflowState workflowState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    @JsonIgnore
    private Sprint sprint;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "time_taken_hours")
    private Double timeTakenHours;

    @com.fasterxml.jackson.annotation.JsonProperty("projectId")
    public Long fetchProjectId() {
        return project != null ? project.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("workflowStateId")
    public Long fetchWorkflowStateId() {
        return workflowState != null ? workflowState.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("boardId")
    public Long fetchBoardId() {
        return board != null ? board.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("sprintId")
    public Long fetchSprintId() {
        return sprint != null ? sprint.getId() : null;
    }
}
