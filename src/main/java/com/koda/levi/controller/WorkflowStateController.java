package com.koda.levi.controller;

import com.koda.levi.dto.WorkflowStateDto;
import com.koda.levi.entity.WorkflowState;
import com.koda.levi.service.WorkflowStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow-states")
@RequiredArgsConstructor
public class WorkflowStateController {

    private final WorkflowStateService workflowStateService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<WorkflowState> createWorkflowState(@Valid @RequestBody WorkflowStateDto workflowStateDto) {
        return ResponseEntity.ok(workflowStateService.create(workflowStateDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowState> getWorkflowStateById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowStateService.getWorkflowStateById(id));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<WorkflowState>> getWorkflowStatesByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(workflowStateService.getAllWorkflowStatesByBoard(boardId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<WorkflowState> updateWorkflowState(@PathVariable Long id, @RequestBody WorkflowStateDto workflowStateDto) {
        return ResponseEntity.ok(workflowStateService.updateWorkflowState(id, workflowStateDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<Void> deleteWorkflowState(@PathVariable Long id) {
        workflowStateService.deleteWorkflowState(id);
        return ResponseEntity.noContent().build();
    }
}
