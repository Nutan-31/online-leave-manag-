package com.leavemanagement.dto;

import com.leavemanagement.entity.Employee;
import com.leavemanagement.entity.LeaveRequest;
import com.leavemanagement.entity.LeaveStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long requestId,
        Long employeeId,
        String employeeName,
        Long reportingManagerId,      // who is expected to review this request
        Long leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDays,
        String reason,
        LeaveStatus status,
        LocalDateTime appliedAt,
        Long approvedById,
        LocalDateTime approvedAt,
        LocalDateTime cancellationRequestedAt,
        Long cancellationApprovedById,
        LocalDateTime cancelledAt
) {
    public static LeaveRequestResponse from(LeaveRequest r) {
        Employee emp = r.getEmployee();
        return new LeaveRequestResponse(
                r.getRequestId(),
                emp.getEmpId(),
                emp.getName(),
                emp.getManager() == null ? null : emp.getManager().getEmpId(),
                r.getLeaveType().getLeaveTypeId(),
                r.getLeaveType().getLeaveTypeName(),
                r.getStartDate(),
                r.getEndDate(),
                r.getTotalDays(),
                r.getReason(),
                r.getStatus(),
                r.getAppliedAt(),
                r.getApprovedBy() == null ? null : r.getApprovedBy().getEmpId(),
                r.getApprovedAt(),
                r.getCancellationRequestedAt(),
                r.getCancellationApprovedBy() == null ? null : r.getCancellationApprovedBy().getEmpId(),
                r.getCancelledAt());
    }
}
