package com.koda.levi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workflow_state")
@Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
public class WorkflowState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @JsonIgnore
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @OneToMany(mappedBy = "workflowState", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Issue> issues;

    @com.fasterxml.jackson.annotation.JsonProperty("boardId")
    public Long fetchBoardId() {
        return board != null ? board.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("organizationId")
    public Long fetchOrganizationId() {
        return organization != null ? organization.getId() : null;
    }
}
