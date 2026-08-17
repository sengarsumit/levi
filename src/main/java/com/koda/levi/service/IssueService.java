package com.koda.levi.service;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Board;
import com.koda.levi.entity.Issue;
import com.koda.levi.entity.Project;
import com.koda.levi.entity.Sprint;
import com.koda.levi.entity.Users;
import com.koda.levi.entity.WorkflowState;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.BoardRepository;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.OrganizationUserRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.repository.SprintRepository;
import com.koda.levi.repository.WorkflowStateRepository;
import com.koda.levi.tenancy.TenantAccessService;
import com.koda.levi.entity.OrganizationUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final IssueTimeCalculatorService timeCalculatorService;
    private final OrganizationUserRepository organizationUserRepository;
    private final ProjectAccessService projectAccessService;

    // TenantContextInterceptor already replaces SecurityContextHolder's Authentication with
    // one carrying live, DB-fresh authorities (and the real Users entity as principal) for
    // the resolved tenant before any controller/service code runs — reading it here follows
    // the same static-accessor style TenantContext.getTenantId() already uses throughout
    // this class, rather than threading caller identity through every method signature.
    private Users currentUser() {
        return (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean callerHasAuthority(String authority) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    private boolean callerIsAdmin() {
        return callerHasAuthority("ORG_ADMIN");
    }

    private boolean callerCanAssign() {
        return callerIsAdmin() || callerHasAuthority("ISSUE_ASSIGN");
    }

    /** Admins see everything; everyone else sees unassigned issues plus their own. */
    private List<Issue> applyVisibility(List<Issue> issues) {
        if (callerIsAdmin()) {
            return issues;
        }
        UUID callerId = currentUser().getId();
        return issues.stream()
                .filter(i -> i.getAssignedTo() == null || i.getAssignedTo().equals(callerId))
                .toList();
    }

    public Issue assignIssue(Integer id, UUID assignedTo) {
        Long tenantId = tenantAccessService.requireTenantId();
        Issue issue = getIssueById(id);

        if (assignedTo != null) {
            organizationUserRepository.findByUserIdAndOrganizationId(assignedTo, tenantId)
                    .orElseThrow(() -> new BadRequestException("assignedTo must be an approved member of this organization"));
        }

        issue.setAssignedTo(assignedTo);
        issue.setAssignedBy(currentUser().getId());
        issue.setUpdatedAt(LocalDateTime.now());
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(new com.koda.levi.event.IssueUpdatedEvent(
            this, saved, "ASSIGNED", saved.getProject().getId()
        ));
        return saved;
    }

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
        // Only honor a caller-supplied assignee if they're actually allowed to assign —
        // otherwise silently create the issue unassigned rather than failing the whole
        // creation over a stray/forged field. Assignment changes on an existing issue only
        // happen through assignIssue()/PATCH /issues/{id}/assign, gated the same way.
        if (issueDto.getAssignedTo() != null && callerCanAssign()) {
            issue.setAssignedTo(issueDto.getAssignedTo());
            issue.setAssignedBy(currentUser().getId());
        }
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
        Issue issue = issueRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));
        // 404 rather than 403 for an issue that exists but isn't visible to this caller —
        // matches TenantAccessService.requireTenant's existing convention of not confirming
        // a resource's existence to someone who shouldn't see it.
        if (!callerIsAdmin() && issue.getAssignedTo() != null && !issue.getAssignedTo().equals(currentUser().getId())) {
            throw new ResourceNotFoundException("Issue not found with id: " + id);
        }
        return issue;
    }

    public List<Issue> getAllIssues() {
        return applyVisibility(issueRepository.findByOrganizationId(tenantAccessService.requireTenantId()));
    }

    public List<Issue> getIssueByOrganisation(long organizationId) {
        tenantAccessService.requireTenant(organizationId);
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
        return applyVisibility(issueRepository.findByProject_Organization_Id(organizationId));
    }

    public List<Issue> getIssuesByProjectId(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        if (projectRepository.findByIdAndOrganizationId(projectId, tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        projectAccessService.requireProjectAccess(projectId);
        return applyVisibility(issueRepository.findByProjectIdAndOrganizationId(projectId, tenantId));
    }

    public List<Issue> getBacklogIssuesByProject(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        if (projectRepository.findByIdAndOrganizationId(projectId, tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        projectAccessService.requireProjectAccess(projectId);
        return applyVisibility(issueRepository.findByProjectIdAndSprintIsNullAndOrganizationId(projectId, tenantId));
    }

    public List<Issue> getIssuesByAssignedUser(UUID userId) {
        // Different shape of guard than applyVisibility: the whole point of this endpoint is
        // "show me X's issues", and within a tenant everyone already knows other members
        // exist, so there's no existence-hiding benefit to a 404 here — a plain 403 is more
        // honest than pretending the concept of "another user's issues" doesn't exist.
        if (!callerIsAdmin() && !userId.equals(currentUser().getId())) {
            throw new AccessDeniedException("Cannot view another user's assigned issues");
        }
        return issueRepository.findByAssignedToAndOrganizationId(userId, tenantAccessService.requireTenantId());
    }

    public List<Issue> getIssuesByWorkflowState(Long workflowStateId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return applyVisibility(issueRepository.findByWorkflowStateIdAndOrganizationId(workflowStateId, tenantId));
    }

    public List<Issue> getIssuesByBoard(Long boardId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return applyVisibility(issueRepository.findByBoardIdAndOrganizationId(boardId, tenantId));
    }

    public Page<Issue> getIssuesWithFilters(Integer status, UUID assignee, Pageable pageable) {
        Long tenantId = tenantAccessService.requireTenantId();
        boolean isAdmin = callerIsAdmin();
        UUID callerId = currentUser().getId();
        return issueRepository.findAll((Specification<Issue>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), tenantId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (assignee != null) {
                // A non-admin asking for someone else's assigned issues would otherwise be a
                // second bypass route around applyVisibility's filtering — fold it into the
                // caller's own visibility predicate instead of ever honoring it verbatim.
                if (!isAdmin && !assignee.equals(callerId)) {
                    predicates.add(criteriaBuilder.disjunction()); // no rows
                } else {
                    predicates.add(criteriaBuilder.equal(root.get("assignedTo"), assignee));
                }
            } else if (!isAdmin) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("assignedTo"), callerId),
                        criteriaBuilder.isNull(root.get("assignedTo"))
                ));
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
        
        String wsName = ws.getName().trim();
        if (wsName.equalsIgnoreCase("In Progress")) {
            // Entering "In Progress" always (re)starts the clock, including when
            // reopening an issue that was previously "Done" — otherwise the stale
            // startedAt/timeTakenHours from the earlier completion would survive and
            // inflate the next elapsed-time calculation by however long the issue sat
            // "Done" in between. This mirrors the same anti-inflation intent as the
            // else branch below, just applied to the reopen case too.
            issue.setStartedAt(LocalDateTime.now());
            issue.setCompletedAt(null);
            issue.setTimeTakenHours(null);
        } else if (wsName.equalsIgnoreCase("Done")) {
            issue.setCompletedAt(LocalDateTime.now());
            if (issue.getStartedAt() != null && issue.getAssignedTo() != null) {
                // Fetch assignee shift
                organizationUserRepository.findByUserIdAndOrganizationId(issue.getAssignedTo(), tenantId).ifPresent(orgUser -> {
                    Double hours = timeCalculatorService.calculateTimeTakenHours(
                        issue.getStartedAt(),
                        issue.getCompletedAt(),
                        orgUser.getShiftStartTime(),
                        orgUser.getShiftEndTime(),
                        orgUser.getWorkingDays()
                    );
                    issue.setTimeTakenHours(hours);
                });
            }
        } else {
            // Moved to neither "In Progress" nor "Done" (e.g. back to "To Do"): clear all
            // progress timestamps so a future "In Progress" transition starts the clock
            // fresh, instead of resuming from the original startedAt and inflating the
            // recorded elapsed time by however long the issue sat idle in between.
            issue.setStartedAt(null);
            issue.setCompletedAt(null);
            issue.setTimeTakenHours(null);
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
        // assignedTo/assignedBy are deliberately never touched here — assignment is the
        // exclusive job of assignIssue()/PATCH /issues/{id}/assign, gated by ISSUE_ASSIGN.
        // ISSUE_UPDATE (this endpoint) is a default permission every member has; letting it
        // also set assignedTo would make that gate meaningless.
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
