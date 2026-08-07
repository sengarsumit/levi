package com.koda.levi.controller;

import com.koda.levi.dto.SprintDto;
import com.koda.levi.entity.Sprint;
import com.koda.levi.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    public ResponseEntity<Sprint> createSprint(@Valid @RequestBody SprintDto sprintDto) {
        return ResponseEntity.ok(sprintService.create(sprintDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sprint> getSprintById(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.getSprintById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Sprint>> getSprintsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(sprintService.getAllSprintsByProject(projectId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sprint> updateSprint(@PathVariable Long id, @Valid @RequestBody SprintDto sprintDto) {
        return ResponseEntity.ok(sprintService.updateSprint(id, sprintDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);
        return ResponseEntity.noContent().build();
    }
}
