package com.leavemanagement.dto;

import com.leavemanagement.entity.ExcessLeaveAction;
import com.leavemanagement.entity.RenewalFrequency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Replaces the policy part of an EmployeeLeave row. It does NOT change the balance. */
public record EmployeeLeavePolicyRequest(
        @NotNull(message = "allocatedDays is required")
        @Positive(message = "allocatedDays must be greater than 0")
        @Digits(integer = 4, fraction = 1, message = "allocatedDays can have at most 1 decimal place")
        BigDecimal allocatedDays,

        @NotNull(message = "renewalFrequency is required (NONE, MONTHLY or ANNUAL)")
        RenewalFrequency renewalFrequency,

        @NotNull(message = "carryForwardAllowed is required")
        Boolean carryForwardAllowed,

        @PositiveOrZero(message = "carryForwardLimit cannot be negative")
        @Digits(integer = 4, fraction = 1, message = "carryForwardLimit can have at most 1 decimal place")
        BigDecimal carryForwardLimit,

        @Positive(message = "maximumBalance must be greater than 0")
        @Digits(integer = 4, fraction = 1, message = "maximumBalance can have at most 1 decimal place")
        BigDecimal maximumBalance,

        @NotNull(message = "excessLeaveAction is required (EXPIRE or ENCASH)")
        ExcessLeaveAction excessLeaveAction
) {
}
