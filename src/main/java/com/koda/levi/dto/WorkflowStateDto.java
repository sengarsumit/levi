package com.koda.levi.dto;

import lombok.Data;

@Data
public class WorkflowStateDto {
    private String name;
    private Integer orderIndex;
    private Long boardId;
}
