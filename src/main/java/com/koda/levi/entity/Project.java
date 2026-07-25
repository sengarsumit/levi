package com.koda.levi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
@Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    /**
     * Alphanumeric project key, e.g. "LEVI-1". Prefix is purely alphabetical;
     * numeric-only prefixes are forbidden per the business ID rule.
     */
    @Column(name = "project_key", nullable = false, unique = true)
    private String projectKey;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** ACTIVE or ARCHIVED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Issue> issue;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    /** Convenience read-only: exposes the organization id in JSON responses */
    @com.fasterxml.jackson.annotation.JsonProperty("organizationId")
    public Long fetchOrganizationId() {
        return organization != null ? organization.getId() : null;
    }
}
