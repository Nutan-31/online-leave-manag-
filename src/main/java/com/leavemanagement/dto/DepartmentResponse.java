package com.leavemanagement.dto;

import com.leavemanagement.entity.Department;
import com.leavemanagement.entity.Employee;

public record DepartmentResponse(
        Long departmentId,
        String departmentName,
        Long departmentManagerId,
        String departmentManagerName
) {
    public static DepartmentResponse from(Department d) {
        Employee m = d.getDepartmentManager();
        return new DepartmentResponse(
                d.getDepartmentId(),
                d.getDepartmentName(),
                m == null ? null : m.getEmpId(),
                m == null ? null : m.getName());
    }
}
