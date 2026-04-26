package com.koda.levi.service;

import com.koda.levi.dto.ProjectDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.entity.Projects;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;

    public Projects create(ProjectDto projectDto) {
        Organization org = organizationRepository.findById(projectDto.getOrgID())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + projectDto.getOrgID()));

        projectRepository.findByName(projectDto.getName())
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
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    public List<Projects> getAllProjects() {
        return projectRepository.findAll();
    }

    public List<Projects> getProjectsByOrganization(Long orgId) {
        return projectRepository.findByOrganizationId(orgId);
    }

    public Projects updateProject(Long id, ProjectDto projectDto) {
        Projects project = getProjectById(id);
        
        if (!project.getName().equals(projectDto.getName())) {
            projectRepository.findByName(projectDto.getName())
                    .ifPresent(p -> {
                        throw new BadRequestException("Project with name " + projectDto.getName() + " already exists");
                    });
        }

        Organization org = organizationRepository.findById(projectDto.getOrgID())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + projectDto.getOrgID()));

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
