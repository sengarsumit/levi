package com.koda.levi.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IssueTimeCalculatorService {

    /**
     * Calculates the business hours spent between startedAt and completedAt,
     * taking into account the user's daily shift and working days.
     */
    public Double calculateTimeTakenHours(
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalTime shiftStartTime,
            LocalTime shiftEndTime,
            String workingDaysStr
    ) {
        if (startedAt == null || completedAt == null || startedAt.isAfter(completedAt)) {
            return 0.0;
        }

        if (shiftStartTime == null) shiftStartTime = LocalTime.of(9, 0);
        if (shiftEndTime == null) shiftEndTime = LocalTime.of(18, 0);
        if (workingDaysStr == null || workingDaysStr.trim().isEmpty()) {
            workingDaysStr = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
        }

        Set<DayOfWeek> workingDays = Arrays.stream(workingDaysStr.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());

        long totalSeconds = 0;
        LocalDate currentDate = startedAt.toLocalDate();
        LocalDate endDate = completedAt.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            if (workingDays.contains(currentDate.getDayOfWeek())) {
                LocalDateTime shiftStart = LocalDateTime.of(currentDate, shiftStartTime);
                LocalDateTime shiftEnd = LocalDateTime.of(currentDate, shiftEndTime);

                LocalDateTime actualStart = (currentDate.equals(startedAt.toLocalDate()))
                        ? (startedAt.isAfter(shiftStart) ? startedAt : shiftStart)
                        : shiftStart;

                LocalDateTime actualEnd = (currentDate.equals(endDate))
                        ? (completedAt.isBefore(shiftEnd) ? completedAt : shiftEnd)
                        : shiftEnd;

                if (actualStart.isBefore(actualEnd)) {
                    totalSeconds += Duration.between(actualStart, actualEnd).getSeconds();
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return Math.round((totalSeconds / 3600.0) * 100.0) / 100.0; // Round to 2 decimal places
    }
}
