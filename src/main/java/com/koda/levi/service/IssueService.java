package com.koda.levi.service;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Issue;
import com.koda.levi.entity.Projects;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    public List<Issue> getIssuesByAssignedUser(UUID userId) {
        return issueRepository.findByAssignedTo(userId);
    }

    public Page<Issue> getIssuesWithFilters(Integer status, UUID assignee, Pageable pageable) {
        return issueRepository.findAll((Specification<Issue>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (assignee != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedTo"), assignee));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public Issue updateIssueStatus(Integer id, Integer status) {
        Issue issue = getIssueById(id);
        issue.setStatus(status);
        issue.setUpdatedAt(LocalDateTime.now());
        return issueRepository.save(issue);
    }

    public Issue updateIssue(Integer id, IssueDto issueDto) {
        Issue issue = getIssueById(id);
        
        Projects project = projectRepository.findById(issueDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + issueDto.getProjectId()));

        issue.setTitle(issueDto.getTitle());
        issue.setDescription(issueDto.getDescription());
        issue.setStatus(issueDto.getStatus());
        issue.setIssueType(issueDto.getIssueType());
        issue.setAssignedTo(issueDto.getAssignedTo());
        issue.setAssignedBy(issueDto.getAssignedBy());
        issue.setProjects(project);
        issue.setUpdatedAt(LocalDateTime.now());

        return issueRepository.save(issue);
    }

    public void deleteIssue(Integer id) {
        Issue issue = getIssueById(id);
        issue.setStatus(5);
        issueRepository.save(issue);
    }
}
