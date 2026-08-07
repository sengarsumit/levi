package com.koda.levi.controller;

import com.koda.levi.dto.CommentDto;
import com.koda.levi.entity.Comment;
import com.koda.levi.entity.Users;
import com.koda.levi.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> createComment(
            @Valid @RequestBody CommentDto dto,
            @AuthenticationPrincipal Users currentUser) {
        UUID userId = currentUser.getId();
        return ResponseEntity.ok(commentService.createComment(dto, userId));
    }

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<Comment>> getCommentsByIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(commentService.getCommentsByIssue(issueId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Users currentUser) {
        UUID userId = currentUser.getId();
        String text = body.get("comment");
        return ResponseEntity.ok(commentService.updateComment(id, text, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Users currentUser) {
        UUID userId = currentUser.getId();
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}
