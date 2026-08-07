package com.koda.levi.controller;

import com.koda.levi.dto.ProjectDto;
import com.koda.levi.entity.Project;
import com.koda.levi.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectDto projectDto) {
        return ResponseEntity.ok(projectService.create(projectDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<List<Project>> getProjectsByOrganization(@PathVariable Long orgId) {
        return ResponseEntity.ok(projectService.getProjectsByOrganization(orgId));
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
}
