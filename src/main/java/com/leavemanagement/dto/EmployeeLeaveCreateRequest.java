package com.leavemanagement.dto;

import com.leavemanagement.entity.ExcessLeaveAction;
import com.leavemanagement.entity.RenewalFrequency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Gives an employee a leave policy + opening balance for one PAID leave type.
 * Everything except employeeId and leaveTypeId is optional and has a default:
 * allocatedDays 15, ANNUAL renewal, no carry forward, no maximum, excess EXPIRES.
 */
public record EmployeeLeaveCreateRequest(
        @NotNull(message = "employeeId is required")
        Long employeeId,

        @NotNull(message = "leaveTypeId is required")
        Long leaveTypeId,

        @Positive(message = "allocatedDays must be greater than 0")
        @Digits(integer = 4, fraction = 1, message = "allocatedDays can have at most 1 decimal place")
        BigDecimal allocatedDays,

        RenewalFrequency renewalFrequency,

        Boolean carryForwardAllowed,

        @PositiveOrZero(message = "carryForwardLimit cannot be negative")
        @Digits(integer = 4, fraction = 1, message = "carryForwardLimit can have at most 1 decimal place")
        BigDecimal carryForwardLimit,

        @Positive(message = "maximumBalance must be greater than 0")
        @Digits(integer = 4, fraction = 1, message = "maximumBalance can have at most 1 decimal place")
        BigDecimal maximumBalance,

        ExcessLeaveAction excessLeaveAction
) {
}
