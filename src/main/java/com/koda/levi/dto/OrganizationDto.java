package com.koda.levi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationDto {
    @NotBlank(message = "Name is required")
    private String name;
}
