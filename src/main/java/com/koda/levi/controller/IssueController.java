package com.koda.levi.controller;

import com.koda.levi.dto.IssueDto;
import com.koda.levi.entity.Issue;
import com.koda.levi.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/org/{id}")
    public ResponseEntity<List<Issue>> getIssueByOrg(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueByOrganisation(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Issue>> getIssuesByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getIssuesByProjectId(projectId));
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
