package com.koda.levi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BoardDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "projectId is required")
    private Long projectId;
}
