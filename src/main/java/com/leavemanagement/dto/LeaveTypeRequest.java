package com.leavemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveTypeRequest(
        @NotBlank(message = "Leave type name is required")
        @Size(max = 100, message = "Leave type name must be at most 100 characters")
        String leaveTypeName,

        @NotNull(message = "isPaid is required (true or false)")
        Boolean isPaid
) {
}
