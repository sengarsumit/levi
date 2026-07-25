package com.koda.levi.controller;

import com.koda.levi.dto.WorkflowStateDto;
import com.koda.levi.entity.WorkflowState;
import com.koda.levi.service.WorkflowStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow-states")
@RequiredArgsConstructor
public class WorkflowStateController {

    private final WorkflowStateService workflowStateService;

    @PostMapping
    public ResponseEntity<WorkflowState> createWorkflowState(@RequestBody WorkflowStateDto workflowStateDto) {
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
    public ResponseEntity<WorkflowState> updateWorkflowState(@PathVariable Long id, @RequestBody WorkflowStateDto workflowStateDto) {
        return ResponseEntity.ok(workflowStateService.updateWorkflowState(id, workflowStateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflowState(@PathVariable Long id) {
        workflowStateService.deleteWorkflowState(id);
        return ResponseEntity.noContent().build();
    }
}
