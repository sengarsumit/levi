package com.koda.levi.service;

import com.koda.levi.dto.CommentDto;
import com.koda.levi.entity.Comment;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.CommentRepository;
import com.koda.levi.repository.IssueRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final TenantAccessService tenantAccessService;

    public Comment createComment(CommentDto dto, UUID userId) {
        Long tenantId = tenantAccessService.requireTenantId();

        // Verify the issue belongs to this tenant
        issueRepository.findByIdAndOrganizationId(dto.getIssueId().intValue(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + dto.getIssueId()));

        if (dto.getComment() == null || dto.getComment().isBlank()) {
            throw new BadRequestException("Comment text cannot be empty");
        }

        Comment comment = new Comment();
        comment.setIssueId(dto.getIssueId());
        comment.setComment(dto.getComment().trim());
        comment.setReplyTo(dto.getReplyTo());
        comment.setUserId(userId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByIssue(Long issueId) {
        Long tenantId = tenantAccessService.requireTenantId();

        // Verify issue belongs to tenant
        issueRepository.findByIdAndOrganizationId(issueId.intValue(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        return commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
    }

    public Comment updateComment(Long id, String text, UUID userId) {
        Comment comment = commentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found or you don't have permission to edit it"));

        if (text == null || text.isBlank()) {
            throw new BadRequestException("Comment text cannot be empty");
        }

        comment.setComment(text.trim());
        comment.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public void deleteComment(Long id, UUID userId) {
        Comment comment = commentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found or you don't have permission to delete it"));
        commentRepository.delete(comment);
    }
}
