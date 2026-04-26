package com.koda.levi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectDto {

    private String name;
    private LocalDateTime deadline;
    private UUID createdBy;
    private LocalDateTime createdAt;

}
