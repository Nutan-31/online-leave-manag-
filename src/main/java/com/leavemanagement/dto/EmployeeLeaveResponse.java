package com.leavemanagement.dto;

import com.leavemanagement.entity.EmployeeLeave;
import com.leavemanagement.entity.ExcessLeaveAction;
import com.leavemanagement.entity.RenewalFrequency;

import java.math.BigDecimal;

public record EmployeeLeaveResponse(
        Long employeeLeaveId,
        Long employeeId,
        String employeeName,
        Long leaveTypeId,
        String leaveTypeName,
        BigDecimal allocatedDays,
        BigDecimal leaveBalance,
        RenewalFrequency renewalFrequency,
        Boolean carryForwardAllowed,
        BigDecimal carryForwardLimit,
        BigDecimal maximumBalance,
        ExcessLeaveAction excessLeaveAction,
        BigDecimal daysUsed
) {
    public static EmployeeLeaveResponse from(EmployeeLeave el) {
        return new EmployeeLeaveResponse(
                el.getEmployeeLeaveId(),
                el.getEmployee().getEmpId(),
                el.getEmployee().getName(),
                el.getLeaveType().getLeaveTypeId(),
                el.getLeaveType().getLeaveTypeName(),
                el.getAllocatedDays(),
                el.getLeaveBalance(),
                el.getRenewalFrequency(),
                el.getCarryForwardAllowed(),
                el.getCarryForwardLimit(),
                el.getMaximumBalance(),
                el.getExcessLeaveAction(),
                el.getDaysUsed());
    }
}
