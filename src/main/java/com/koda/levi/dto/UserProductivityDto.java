package com.koda.levi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProductivityDto {
    private UUID userId;
    private long assignedCount;
    private long completedCount;
    private Double avgTimeTakenHours;
}
