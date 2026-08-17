package com.koda.levi.controller;

import com.koda.levi.dto.ProjectDto;
import com.koda.levi.dto.ProjectMemberDto;
import com.koda.levi.entity.Project;
import com.koda.levi.service.ProjectAccessService;
import com.koda.levi.service.ProjectMemberService;
import com.koda.levi.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final ProjectAccessService projectAccessService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectDto projectDto) {
        return ResponseEntity.ok(projectService.create(projectDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        // Access gating lives at the controller boundary rather than inside the @Cacheable
        // service method: an in-method check would be skipped on a cache hit (the body never
        // runs), so a non-member could read a project another caller had already cached.
        projectAccessService.requireProjectAccess(id);
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(filterAccessible(projectService.getAllProjects()));
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<List<Project>> getProjectsByOrganization(@PathVariable Long orgId) {
        return ResponseEntity.ok(filterAccessible(projectService.getProjectsByOrganization(orgId)));
    }

    /**
     * Non-admins see only projects they belong to; admins (null sentinel) see all. Applied
     * here, after the cached raw per-tenant list is returned, so the cache stays caller-agnostic.
     */
    private List<Project> filterAccessible(List<Project> projects) {
        List<Long> accessible = projectAccessService.accessibleProjectIds();
        if (accessible == null) {
            return projects;
        }
        return projects.stream()
                .filter(p -> accessible.contains(p.getId()))
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectDto projectDto) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberDto>> getMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectMemberService.getMembers(projectId));
    }

    @PostMapping("/{projectId}/members")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<ProjectMemberDto> addMember(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(projectMemberService.addMember(projectId, UUID.fromString(userId)));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<Void> removeMember(@PathVariable Long projectId, @PathVariable UUID userId) {
        projectMemberService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}
