package com.koda.levi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentDto {
    @NotNull(message = "issueId is required")
    private Long issueId;

    @NotBlank(message = "Comment text is required")
    private String comment;

    private Long replyTo; // nullable — parent comment id for thread replies
}
