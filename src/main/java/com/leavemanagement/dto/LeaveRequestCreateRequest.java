package com.leavemanagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestCreateRequest(
        @NotNull(message = "employeeId is required")
        Long employeeId,

        @NotNull(message = "leaveTypeId is required")
        Long leaveTypeId,

        @NotNull(message = "startDate is required (yyyy-MM-dd)")
        LocalDate startDate,

        @NotNull(message = "endDate is required (yyyy-MM-dd)")
        LocalDate endDate,

        // Optional. Leave it out to take all working days in the range.
        // Use it for half days: 0.5, 1.5, 2.5 ...
        @DecimalMin(value = "0.5", message = "totalDays must be at least 0.5")
        @Digits(integer = 4, fraction = 1, message = "totalDays can have at most 1 decimal place")
        BigDecimal totalDays,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
