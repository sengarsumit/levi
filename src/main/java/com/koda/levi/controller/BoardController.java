package com.koda.levi.controller;

import com.koda.levi.dto.BoardDto;
import com.koda.levi.entity.Board;
import com.koda.levi.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

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
    public ResponseEntity<Board> updateBoard(@PathVariable Long id, @RequestBody BoardDto boardDto) {
        return ResponseEntity.ok(boardService.updateBoard(id, boardDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
