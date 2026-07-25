package com.koda.levi.dto;

import lombok.Data;

@Data
public class CommentDto {
    private Long issueId;
    private String comment;
    private Long replyTo; // nullable — parent comment id for thread replies
}
