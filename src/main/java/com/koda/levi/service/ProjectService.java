package com.koda.levi.service;

import com.koda.levi.dto.ProjectDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.entity.Project;
import com.koda.levi.entity.Users;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;
    private final ProjectMemberService projectMemberService;

    private UUID currentUserId() {
        return ((Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    @CacheEvict(value = {"projects", "organizationProjects"}, allEntries = true)
    public Project create(ProjectDto projectDto) {
        Long tenantId = tenantAccessService.requireTenantId();
        tenantAccessService.requireTenant(projectDto.getOrgId());

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        projectRepository.findByNameAndOrganizationId(projectDto.getName(), tenantId)
                .ifPresent(p -> {
                    throw new BadRequestException("Project with name " + projectDto.getName() + " already exists");
                });

        Project project = new Project();
        project.setName(projectDto.getName());
        
        if (projectDto.getProjectKey() != null) {
            project.setProjectKey(projectDto.getProjectKey());
        } else {
            project.setProjectKey("PRJ-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        
        if (projectDto.getDescription() != null) {
            project.setDescription(projectDto.getDescription());
        }
        
        if (projectDto.getStatus() != null) {
            project.setStatus(projectDto.getStatus());
        } else {
            project.setStatus(com.koda.levi.entity.ProjectStatus.ACTIVE);
        }
        
        project.setDeadline(projectDto.getDeadline());
        // createdBy is taken from the authenticated caller, never the client-supplied DTO
        // field (which is spoofable) — this is also who gets auto-enrolled as a member below.
        UUID creatorId = currentUserId();
        project.setCreatedBy(creatorId);
        project.setOrganization(org);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        Project saved = projectRepository.save(project);
        projectMemberService.addMemberInternal(saved, creatorId);
        return saved;
    }

    @Cacheable(value = "projects", key = "T(com.koda.levi.tenancy.TenantContext).getTenantId() + '-' + #id")
    public Project getProjectById(Long id) {
        Long tenantId = tenantAccessService.requireTenantId();
        return projectRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    @Cacheable(value = "organizationProjects", key = "T(com.koda.levi.tenancy.TenantContext).getTenantId()")
    public List<Project> getAllProjects() {
        return projectRepository.findByOrganizationId(tenantAccessService.requireTenantId());
    }

    public List<Project> getProjectsByOrganization(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        if (!organizationRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }
        return projectRepository.findByOrganizationId(orgId);
    }

    @CacheEvict(value = {"projects", "organizationProjects"}, allEntries = true)
    public Project updateProject(Long id, ProjectDto projectDto) {
        Project project = getProjectById(id);
        Long tenantId = tenantAccessService.requireTenantId();
        tenantAccessService.requireTenant(projectDto.getOrgId());
        
        if (!project.getName().equals(projectDto.getName())) {
            projectRepository.findByNameAndOrganizationId(projectDto.getName(), tenantId)
                    .ifPresent(p -> {
                        throw new BadRequestException("Project with name " + projectDto.getName() + " already exists");
                    });
        }

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        project.setName(projectDto.getName());
        if (projectDto.getDescription() != null) {
            project.setDescription(projectDto.getDescription());
        }
        if (projectDto.getStatus() != null) {
            project.setStatus(projectDto.getStatus());
        }
        project.setDeadline(projectDto.getDeadline());
        project.setOrganization(org);
        project.setUpdatedAt(LocalDateTime.now());
        
        return projectRepository.save(project);
    }

    @CacheEvict(value = {"projects", "organizationProjects"}, allEntries = true)
    public void deleteProject(Long id) {
        Project project = getProjectById(id);
        projectRepository.delete(project);
    }
}
