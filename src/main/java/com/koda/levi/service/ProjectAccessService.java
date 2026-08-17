package com.koda.levi.service;

import com.koda.levi.entity.Users;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Single chokepoint for project-level access decisions so gating is applied consistently
 * across every project-scoped read path (project list/detail, issues, backlog, productivity)
 * rather than duplicated — a filtered list with an ungated /issues/project/{id} would be a
 * security hole, not a feature.
 *
 * Admins (ORG_ADMIN) always have access; everyone else is limited to projects they are a
 * member of. Reads the caller from the SecurityContext the same way IssueService does — the
 * TenantContextInterceptor has already installed a DB-fresh Authentication for the request.
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    private Users currentUser() {
        return (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean callerIsAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ORG_ADMIN"));
    }

    public boolean canAccess(Long projectId) {
        if (callerIsAdmin()) {
            return true;
        }
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser().getId());
    }

    /**
     * 404 (not 403) on denial: consistent with the tenant-miss behavior elsewhere, it avoids
     * revealing that a project the caller isn't on even exists.
     */
    public void requireProjectAccess(Long projectId) {
        if (!canAccess(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
    }

    /**
     * Returns null for admins as a "no restriction" sentinel (see all projects); otherwise the
     * ids of the projects the caller belongs to. Callers must null-check before filtering.
     */
    public List<Long> accessibleProjectIds() {
        if (callerIsAdmin()) {
            return null;
        }
        return projectMemberRepository.findProjectIdsByUserId(currentUser().getId());
    }
}
