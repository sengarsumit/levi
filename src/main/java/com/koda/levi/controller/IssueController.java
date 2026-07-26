package com.koda.levi.controller;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Issue;
import com.koda.levi.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<Issue> createIssue(@RequestBody IssueDto issueDto) {
        return ResponseEntity.ok(issueService.createIssue(issueDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Issue> getIssueById(@PathVariable Integer id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        return ResponseEntity.ok(issueService.getAllIssues());
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<Issue>> getIssuesWithFilters(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) UUID assignee,
            Pageable pageable) {
        return ResponseEntity.ok(issueService.getIssuesWithFilters(status, assignee, pageable));
    }

    @GetMapping("/org/{id}")
    public ResponseEntity<List<Issue>> getIssueByOrg(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueByOrganisation(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Issue>> getIssuesByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getIssuesByProjectId(projectId));
    }

    @GetMapping("/project/{projectId}/backlog")
    public ResponseEntity<List<Issue>> getBacklogIssuesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getBacklogIssuesByProject(projectId));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Issue>> getIssuesByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(issueService.getIssuesByBoard(boardId));
    }

    @GetMapping("/workflow-state/{workflowStateId}")
    public ResponseEntity<List<Issue>> getIssuesByWorkflowState(@PathVariable Long workflowStateId) {
        return ResponseEntity.ok(issueService.getIssuesByWorkflowState(workflowStateId));
    }

    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<Issue>> getIssuesByAssignedUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(issueService.getIssuesByAssignedUser(userId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TASK_TRANSITION')")
    public ResponseEntity<Issue> updateIssueStatus(@PathVariable Integer id, @RequestBody Integer status) {
        return ResponseEntity.ok(issueService.updateIssueStatus(id, status));
    }

    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAuthority('TASK_TRANSITION')")
    public ResponseEntity<Issue> moveIssue(@PathVariable Integer id, @RequestBody Map<String, Long> body) {
        Long workflowStateId = body.get("workflowStateId");
        if (workflowStateId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(issueService.moveIssue(id, workflowStateId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Issue> updateIssue(@PathVariable Integer id, @RequestBody IssueDto issueDto) {
        return ResponseEntity.ok(issueService.updateIssue(id, issueDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Integer id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }
}
