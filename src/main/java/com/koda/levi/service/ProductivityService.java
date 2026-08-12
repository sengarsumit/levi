package com.koda.levi.service;

import com.koda.levi.dto.ProductivityStatsDto;
import com.koda.levi.dto.UserProductivityDto;
import com.koda.levi.entity.Issue;
import com.koda.levi.entity.Users;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductivityService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final TenantAccessService tenantAccessService;

    private Users currentUser() {
        return (Users) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean callerIsAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ORG_ADMIN"));
    }

    public ProductivityStatsDto getProjectProductivity(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        projectRepository.findByIdAndOrganizationId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Deliberately NOT filtered through IssueService.applyVisibility() here — that hides
        // other people's issues from a *list* view, but every issue should still contribute to
        // these totals regardless of caller. Only the per-user breakdown below is access-restricted,
        // and that happens after aggregation, not before.
        List<Issue> issues = issueRepository.findByProjectIdAndOrganizationId(projectId, tenantId);

        long totalIssues = issues.size();
        List<Issue> completed = issues.stream().filter(i -> i.getCompletedAt() != null).toList();
        long completedIssues = completed.size();
        Double avgTimeTakenHours = averageOf(completed);

        Map<UUID, List<Issue>> byAssignee = issues.stream()
                .filter(i -> i.getAssignedTo() != null)
                .collect(Collectors.groupingBy(Issue::getAssignedTo));

        List<UserProductivityDto> byUser = byAssignee.entrySet().stream()
                .map(e -> toUserProductivity(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(UserProductivityDto::getCompletedCount).reversed())
                .toList();

        if (!callerIsAdmin()) {
            UUID callerId = currentUser().getId();
            byUser = byUser.stream().filter(u -> u.getUserId().equals(callerId)).toList();
        }

        return ProductivityStatsDto.builder()
                .totalIssues(totalIssues)
                .completedIssues(completedIssues)
                .avgTimeTakenHours(avgTimeTakenHours)
                .byUser(byUser)
                .build();
    }

    private UserProductivityDto toUserProductivity(UUID userId, List<Issue> userIssues) {
        List<Issue> completed = userIssues.stream().filter(i -> i.getCompletedAt() != null).toList();
        return UserProductivityDto.builder()
                .userId(userId)
                .assignedCount(userIssues.size())
                .completedCount(completed.size())
                .avgTimeTakenHours(averageOf(completed))
                .build();
    }

    // Averages only issues with a non-null timeTakenHours (e.g. an unassigned issue can reach
    // Done with no shift data, hence no computed time) — excluded rather than treated as zero.
    private Double averageOf(List<Issue> completedIssues) {
        OptionalDouble avg = completedIssues.stream()
                .map(Issue::getTimeTakenHours)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return avg.isPresent() ? avg.getAsDouble() : null;
    }
}
