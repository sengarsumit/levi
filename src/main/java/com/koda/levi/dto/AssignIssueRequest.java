package com.koda.levi.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AssignIssueRequest {
    // null means unassign
    private UUID assignedTo;
}
