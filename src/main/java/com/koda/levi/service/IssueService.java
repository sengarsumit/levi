package com.koda.levi.service;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Board;
import com.koda.levi.entity.Issue;
import com.koda.levi.entity.Project;
import com.koda.levi.entity.Sprint;
import com.koda.levi.entity.WorkflowState;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.BoardRepository;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.repository.SprintRepository;
import com.koda.levi.repository.WorkflowStateRepository;
import com.koda.levi.tenancy.TenantAccessService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final WorkflowStateRepository workflowStateRepository;
    private final BoardRepository boardRepository;
    private final SprintRepository sprintRepository;
    private final TenantAccessService tenantAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public Issue createIssue(IssueDto issueDto) {
        Long tenantId = tenantAccessService.requireTenantId();
        Project project = projectRepository.findByIdAndOrganizationId(issueDto.getProjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + issueDto.getProjectId()));

        String issueType = normalizeIssueType(issueDto.getIssueType());
        Issue issue = new Issue();
        issue.setTitle(issueDto.getTitle());
        issue.setDescription(issueDto.getDescription());
        issue.setStatus(issueDto.getStatus() != null ? issueDto.getStatus() : 0);
        issue.setIssueType(issueType);
        issue.setAssignedTo(issueDto.getAssignedTo());
        issue.setAssignedBy(issueDto.getAssignedBy());
        issue.setProject(project);
        issue.setOrganization(project.getOrganization());

        // Wire workflowState if provided
        if (issueDto.getWorkflowStateId() != null) {
            WorkflowState ws = workflowStateRepository.findByIdAndOrganizationId(issueDto.getWorkflowStateId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found with id: " + issueDto.getWorkflowStateId()));
            issue.setWorkflowState(ws);
            if (ws.getBoard() != null) {
                issue.setBoard(ws.getBoard());
            }
        }

        // Override board if explicitly provided
        if (issueDto.getBoardId() != null) {
            Board board = boardRepository.findByIdAndOrganizationId(issueDto.getBoardId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + issueDto.getBoardId()));
            issue.setBoard(board);
        }

        // Assign sprint if provided
        if (issueDto.getSprintId() != null) {
            Sprint sprint = sprintRepository.findByIdAndOrganizationId(issueDto.getSprintId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + issueDto.getSprintId()));
            issue.setSprint(sprint);
        }

        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());

        Issue saved = issueRepository.save(issue);
        
        eventPublisher.publishEvent(new com.koda.levi.event.IssueUpdatedEvent(
            this, saved, "CREATED", project.getId()
        ));
        
        return saved;
    }

    public Issue getIssueById(Integer id) {
        Long tenantId = tenantAccessService.requireTenantId();
        return issueRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findByOrganizationId(tenantAccessService.requireTenantId());
    }

    public List<Issue> getIssueByOrganisation(long organizationId) {
        tenantAccessService.requireTenant(organizationId);
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
        return issueRepository.findByProject_Organization_Id(organizationId);
    }

    public List<Issue> getIssuesByProjectId(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        if (projectRepository.findByIdAndOrganizationId(projectId, tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return issueRepository.findByProjectIdAndOrganizationId(projectId, tenantId);
    }

    public List<Issue> getBacklogIssuesByProject(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        if (projectRepository.findByIdAndOrganizationId(projectId, tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return issueRepository.findByProjectIdAndSprintIsNullAndOrganizationId(projectId, tenantId);
    }

    public List<Issue> getIssuesByAssignedUser(UUID userId) {
        return issueRepository.findByAssignedToAndOrganizationId(userId, tenantAccessService.requireTenantId());
    }

    public List<Issue> getIssuesByWorkflowState(Long workflowStateId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return issueRepository.findByWorkflowStateIdAndOrganizationId(workflowStateId, tenantId);
    }

    public List<Issue> getIssuesByBoard(Long boardId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return issueRepository.findByBoardIdAndOrganizationId(boardId, tenantId);
    }

    public Page<Issue> getIssuesWithFilters(Integer status, UUID assignee, Pageable pageable) {
        Long tenantId = tenantAccessService.requireTenantId();
        return issueRepository.findAll((Specification<Issue>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), tenantId));

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
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(new com.koda.levi.event.IssueUpdatedEvent(
            this, saved, "STATUS_UPDATED", saved.getProject().getId()
        ));
        return saved;
    }

    public Issue moveIssue(Integer id, Long workflowStateId) {
        Long tenantId = tenantAccessService.requireTenantId();
        Issue issue = getIssueById(id);
        WorkflowState ws = workflowStateRepository.findByIdAndOrganizationId(workflowStateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found with id: " + workflowStateId));
        issue.setWorkflowState(ws);
        if (ws.getBoard() != null) {
            issue.setBoard(ws.getBoard());
        }
        issue.setUpdatedAt(LocalDateTime.now());
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(new com.koda.levi.event.IssueUpdatedEvent(
            this, saved, "MOVED", saved.getProject().getId()
        ));
        return saved;
    }

    public Issue updateIssue(Integer id, IssueDto issueDto) {
        Issue issue = getIssueById(id);
        Long tenantId = tenantAccessService.requireTenantId();

        Project project = projectRepository.findByIdAndOrganizationId(issueDto.getProjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + issueDto.getProjectId()));

        issue.setTitle(issueDto.getTitle());
        issue.setDescription(issueDto.getDescription());
        issue.setStatus(issueDto.getStatus() != null ? issueDto.getStatus() : issue.getStatus());
        issue.setIssueType(normalizeIssueType(issueDto.getIssueType()));
        issue.setAssignedTo(issueDto.getAssignedTo());
        issue.setAssignedBy(issueDto.getAssignedBy());
        issue.setProject(project);
        issue.setOrganization(project.getOrganization());

        if (issueDto.getWorkflowStateId() != null) {
            WorkflowState ws = workflowStateRepository.findByIdAndOrganizationId(issueDto.getWorkflowStateId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found with id: " + issueDto.getWorkflowStateId()));
            issue.setWorkflowState(ws);
            if (ws.getBoard() != null) {
                issue.setBoard(ws.getBoard());
            }
        }

        if (issueDto.getSprintId() != null) {
            Sprint sprint = sprintRepository.findByIdAndOrganizationId(issueDto.getSprintId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + issueDto.getSprintId()));
            issue.setSprint(sprint);
        } else {
            issue.setSprint(null); // Allows removing issue from sprint
        }

        issue.setUpdatedAt(LocalDateTime.now());

        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(new com.koda.levi.event.IssueUpdatedEvent(
            this, saved, "UPDATED", project.getId()
        ));
        return saved;
    }

    public void deleteIssue(Integer id) {
        Issue issue = getIssueById(id);
        Long projectId = issue.getProject().getId();
        issueRepository.delete(issue);
        eventPublisher.publishEvent(new com.koda.levi.event.IssueUpdatedEvent(
            this, issue, "DELETED", projectId
        ));
    }

    private String normalizeIssueType(String issueType) {
        if (issueType == null || issueType.isBlank()) {
            throw new BadRequestException("Issue type is required");
        }

        String normalized = issueType.trim().toUpperCase();
        if (Stream.of("BUG", "TASK", "STORY").noneMatch(type -> type.equals(normalized))) {
            throw new BadRequestException("Issue type must be one of BUG, TASK, STORY");
        }

        return normalized;
    }
}
