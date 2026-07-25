package com.koda.levi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SprintDto {
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long projectId;
}
