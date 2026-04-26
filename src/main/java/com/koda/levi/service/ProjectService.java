package com.koda.levi.service;

import com.koda.levi.dto.ProjectDto;
import com.koda.levi.entity.Projects;
import com.koda.levi.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public Projects create(ProjectDto projectDto) {
        Projects project = new Projects();
        project.setName(projectDto.getName());
        project.setCreatedBy(projectDto.getCreatedBy());
        project.setCreatedAt(projectDto.getCreatedAt());
        projectRepository.save(project);
        return project;
    }
}
