package com.koda.levi.service;

import com.koda.levi.dto.ProjectMemberDto;
import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.entity.Project;
import com.koda.levi.entity.ProjectMember;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationUserRepository;
import com.koda.levi.repository.ProjectMemberRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ProjectAccessService projectAccessService;
    private final TenantAccessService tenantAccessService;

    private Project requireProjectInTenant(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return projectRepository.findByIdAndOrganizationId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    public List<ProjectMemberDto> getMembers(Long projectId) {
        requireProjectInTenant(projectId);
        projectAccessService.requireProjectAccess(projectId);
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(pm -> ProjectMemberDto.builder()
                        .userId(pm.getUserId())
                        .createdAt(pm.getCreatedAt())
                        .build())
                .toList();
    }

    public ProjectMemberDto addMember(Long projectId, UUID userId) {
        Project project = requireProjectInTenant(projectId);
        Long tenantId = tenantAccessService.requireTenantId();

        // Only approved members of this org can be added — mirrors the assignee check in
        // IssueService.assignIssue so a project roster can never reference an outsider.
        organizationUserRepository.findByUserIdAndOrganizationId(userId, tenantId)
                .filter(ou -> ou.getStatus() == OrgUserStatus.APPROVED)
                .orElseThrow(() -> new BadRequestException("User must be an approved member of this organization"));

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            ProjectMember member = new ProjectMember();
            member.setProject(project);
            member.setOrganization(project.getOrganization());
            member.setUserId(userId);
            projectMemberRepository.save(member);
        }

        return ProjectMemberDto.builder().userId(userId).build();
    }

    public void removeMember(Long projectId, UUID userId) {
        requireProjectInTenant(projectId);
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    /** Auto-enroll a user (typically the creator) without the admin-only gate. */
    public void addMemberInternal(Project project, UUID userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            ProjectMember member = new ProjectMember();
            member.setProject(project);
            member.setOrganization(project.getOrganization());
            member.setUserId(userId);
            projectMemberRepository.save(member);
        }
    }
}
