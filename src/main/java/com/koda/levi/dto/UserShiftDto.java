package com.koda.levi.dto;

import lombok.Data;

@Data
public class UserShiftDto {
    private String shiftStartTime; // e.g. "09:00"
    private String shiftEndTime; // e.g. "18:00"
    private String workingDays; // e.g. "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
}
