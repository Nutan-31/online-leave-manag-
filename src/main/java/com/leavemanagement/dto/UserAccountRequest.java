package com.leavemanagement.dto;

import com.leavemanagement.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserAccountRequest(
        // Optional only for ADMIN accounts
        Long employeeId,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be 8 to 100 characters")
        String password,

        @NotNull(message = "Role is required (ADMIN, MANAGER or EMPLOYEE)")
        Role role
) {
}
