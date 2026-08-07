package com.koda.levi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowStateDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "orderIndex is required")
    private Integer orderIndex;

    @NotNull(message = "boardId is required")
    private Long boardId;
}
