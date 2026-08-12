package com.koda.levi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityStatsDto {
    private long totalIssues;
    private long completedIssues;
    private Double avgTimeTakenHours;
    private List<UserProductivityDto> byUser;
}
