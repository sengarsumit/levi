package com.koda.levi.dto;

import com.koda.levi.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectDto {

    @NotBlank(message = "Name is required")
    private String name;
    private String projectKey;
    private String description;
    private ProjectStatus status;
    private LocalDateTime deadline;
    private UUID createdBy;
    private long orgId;
}

