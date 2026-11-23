package com.leavemanagement.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin adjustment. Positive adds days, negative removes days. */
public record BalanceAdjustmentRequest(
        @NotNull(message = "days is required (e.g. 2 or -1.5)")
        @Digits(integer = 4, fraction = 1, message = "days can have at most 1 decimal place")
        BigDecimal days
) {
}
