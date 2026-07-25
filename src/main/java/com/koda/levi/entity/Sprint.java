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
@Table(name = "sprint")
@Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
public class Sprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Issue> issues;

    @com.fasterxml.jackson.annotation.JsonProperty("projectId")
    public Long fetchProjectId() {
        return project != null ? project.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("organizationId")
    public Long fetchOrganizationId() {
        return organization != null ? organization.getId() : null;
    }
}
