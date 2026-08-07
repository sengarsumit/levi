package com.koda.levi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SprintDto {
    @NotBlank(message = "Name is required")
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Long projectId;
}
