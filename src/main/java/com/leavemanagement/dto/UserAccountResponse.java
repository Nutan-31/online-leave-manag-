package com.leavemanagement.dto;

import com.leavemanagement.entity.AccountStatus;
import com.leavemanagement.entity.Employee;
import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;

/** Note: the password hash is deliberately NOT part of this response. */
public record UserAccountResponse(
        Long accountId,
        Long employeeId,
        String employeeName,
        String username,
        Role role,
        AccountStatus accountStatus
) {
    public static UserAccountResponse from(UserAccount a) {
        Employee e = a.getEmployee();
        return new UserAccountResponse(
                a.getAccountId(),
                e == null ? null : e.getEmpId(),
                e == null ? null : e.getName(),
                a.getUsername(),
                a.getRole(),
                a.getAccountStatus());
    }
}
