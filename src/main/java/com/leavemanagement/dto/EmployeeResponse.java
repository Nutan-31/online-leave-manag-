package com.leavemanagement.dto;

import com.leavemanagement.entity.Employee;

import java.time.LocalDate;

public record EmployeeResponse(
        Long empId,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        String designation,
        Long managerId,        // REPORTING manager
        String managerName,
        LocalDate dateOfJoining
) {
    public static EmployeeResponse from(Employee e) {
        Employee m = e.getManager();
        return new EmployeeResponse(
                e.getEmpId(),
                e.getName(),
                e.getEmail(),
                e.getDepartment().getDepartmentId(),
                e.getDepartment().getDepartmentName(),
                e.getDesignation(),
                m == null ? null : m.getEmpId(),
                m == null ? null : m.getName(),
                e.getDateOfJoining());
    }
}
