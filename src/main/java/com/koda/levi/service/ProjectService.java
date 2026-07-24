package com.koda.levi.service;

import com.koda.levi.dto.ProjectDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.entity.Projects;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    public Projects create(ProjectDto projectDto) {
        Long tenantId = tenantAccessService.requireTenantId();
        tenantAccessService.requireTenant(projectDto.getOrgID());

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        projectRepository.findByNameAndOrganizationId(projectDto.getName(), tenantId)
                .ifPresent(p -> {
                    throw new BadRequestException("Project with name " + projectDto.getName() + " already exists");
                });

        Projects project = new Projects();
        project.setName(projectDto.getName());
        project.setDeadline(projectDto.getDeadline());
        project.setCreatedBy(projectDto.getCreatedBy());
        project.setOrganization(org);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        
        return projectRepository.save(project);
    }

    public Projects getProjectById(Long id) {
        Long tenantId = tenantAccessService.requireTenantId();
        return projectRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    public List<Projects> getAllProjects() {
        return projectRepository.findByOrganizationId(tenantAccessService.requireTenantId());
    }

    public List<Projects> getProjectsByOrganization(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        if (!organizationRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }
        return projectRepository.findByOrganizationId(orgId);
    }

    public Projects updateProject(Long id, ProjectDto projectDto) {
        Projects project = getProjectById(id);
        Long tenantId = tenantAccessService.requireTenantId();
        tenantAccessService.requireTenant(projectDto.getOrgID());
        
        if (!project.getName().equals(projectDto.getName())) {
            projectRepository.findByNameAndOrganizationId(projectDto.getName(), tenantId)
                    .ifPresent(p -> {
                        throw new BadRequestException("Project with name " + projectDto.getName() + " already exists");
                    });
        }

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        project.setName(projectDto.getName());
        project.setDeadline(projectDto.getDeadline());
        project.setOrganization(org);
        project.setUpdatedAt(LocalDateTime.now());
        
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        Projects project = getProjectById(id);
        projectRepository.delete(project);
    }
}
