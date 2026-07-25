package com.koda.levi.service;

import com.koda.levi.dto.SprintDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.entity.Project;
import com.koda.levi.entity.Sprint;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.repository.SprintRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    @CacheEvict(value = "sprints", key = "#sprintDto.projectId")
    public Sprint create(SprintDto sprintDto) {
        Long tenantId = tenantAccessService.requireTenantId();

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        Project project = projectRepository.findByIdAndOrganizationId(sprintDto.getProjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + sprintDto.getProjectId()));

        Sprint sprint = new Sprint();
        sprint.setName(sprintDto.getName());
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());
        sprint.setProject(project);
        sprint.setOrganization(org);
        sprint.setCreatedAt(LocalDateTime.now());
        sprint.setUpdatedAt(LocalDateTime.now());

        return sprintRepository.save(sprint);
    }

    public Sprint getSprintById(Long id) {
        Long tenantId = tenantAccessService.requireTenantId();
        return sprintRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));
    }

    @Cacheable(value = "sprints", key = "#projectId")
    public List<Sprint> getAllSprintsByProject(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return sprintRepository.findByProjectIdAndOrganizationId(projectId, tenantId);
    }

    @CacheEvict(value = "sprints", allEntries = true)
    public Sprint updateSprint(Long id, SprintDto sprintDto) {
        Sprint sprint = getSprintById(id);
        
        sprint.setName(sprintDto.getName());
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());
        sprint.setUpdatedAt(LocalDateTime.now());
        
        return sprintRepository.save(sprint);
    }

    @CacheEvict(value = "sprints", allEntries = true)
    public void deleteSprint(Long id) {
        Sprint sprint = getSprintById(id);
        sprintRepository.delete(sprint);
    }
}
