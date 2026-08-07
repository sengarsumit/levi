package com.koda.levi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class IssueDto {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private Integer status;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    private UUID assignedTo;
    private UUID assignedBy;

    @NotNull(message = "projectId is required")
    private Long projectId;

    private Long workflowStateId;
    private Long boardId;
    private Long sprintId;
}
