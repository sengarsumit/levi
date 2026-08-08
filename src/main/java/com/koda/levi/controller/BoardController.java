package com.koda.levi.controller;

import com.koda.levi.dto.BoardDto;
import com.koda.levi.entity.Board;
import com.koda.levi.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // Intentionally not permission-gated: BoardPage.tsx auto-creates a project's default
    // board the first time ANY member opens it (whichever role happens to view it first),
    // not just admins. Gating this would leave the board permanently stuck on "being set
    // up..." for any org where a non-admin gets there before an admin does.
    @PostMapping
    public ResponseEntity<Board> createBoard(@Valid @RequestBody BoardDto boardDto) {
        return ResponseEntity.ok(boardService.create(boardDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Board>> getBoardsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(boardService.getAllBoardsByProject(projectId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<Board> updateBoard(@PathVariable Long id, @RequestBody BoardDto boardDto) {
        return ResponseEntity.ok(boardService.updateBoard(id, boardDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
