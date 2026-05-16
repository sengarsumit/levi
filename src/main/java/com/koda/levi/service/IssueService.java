package com.koda.levi.service;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Issue;
import com.koda.levi.entity.Projects;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;

    public Issue createIssue(IssueDto issueDto) {
        Projects project = projectRepository.findById(issueDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + issueDto.getProjectId()));

        String issueType= String.valueOf(Stream.of("BUG","TASK","STORY").anyMatch(type-> type.equalsIgnoreCase(issueDto.getIssueType())));
        Issue issue = new Issue();
        issue.setTitle(issueDto.getTitle());
        issue.setDescription(issueDto.getDescription());
        issue.setStatus(issueDto.getStatus());
        issue.setIssueType(issueType);
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

    public List<Issue> getIssueByOrganisation(long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
        return issueRepository.findByProjects_Organization_Id(organizationId);
    }

    public List<Issue> getIssuesByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
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
