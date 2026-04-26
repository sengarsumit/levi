package com.koda.levi.service;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Issue;
import com.koda.levi.entity.Projects;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;

    public Issue createIssue(IssueDto issueDto) {
        Projects project = projectRepository.findById(issueDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + issueDto.getProjectId()));

        Issue issue = new Issue();
        issue.setTitle(issueDto.getTitle());
        issue.setDescription(issueDto.getDescription());
        issue.setStatus(issueDto.getStatus());
        issue.setAssignedTo(issueDto.getAssignedTo());
        issue.setAssignedBy(issueDto.getAssignedBy());
        issue.setProjects(project);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());

        return issueRepository.save(issue);
    }

    public Issue getIssueById(Integer id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    public List<Issue> getIssuesByProjectId(Long projectId) {
        return issueRepository.findByProjectsId(projectId);
    }

    public Issue updateIssue(Integer id, IssueDto issueDto) {
        Issue issue = getIssueById(id);
        
        Projects project = projectRepository.findById(issueDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + issueDto.getProjectId()));

        issue.setTitle(issueDto.getTitle());
        issue.setDescription(issueDto.getDescription());
        issue.setStatus(issueDto.getStatus());
        issue.setAssignedTo(issueDto.getAssignedTo());
        issue.setAssignedBy(issueDto.getAssignedBy());
        issue.setProjects(project);
        issue.setUpdatedAt(LocalDateTime.now());

        return issueRepository.save(issue);
    }

    public void deleteIssue(Integer id) {
        Issue issue = getIssueById(id);
        issueRepository.delete(issue);
    }
}
