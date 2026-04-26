package com.koda.levi.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class IssueDto {
    private String title;
    private String description;
    private Integer status;
    private UUID assignedTo;
    private UUID assignedBy;
    private Long projectId;
}
