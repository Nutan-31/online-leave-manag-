package com.leavemanagement.dto;

import com.leavemanagement.entity.LeaveType;

public record LeaveTypeResponse(
        Long leaveTypeId,
        String leaveTypeName,
        Boolean isPaid
) {
    public static LeaveTypeResponse from(LeaveType t) {
        return new LeaveTypeResponse(t.getLeaveTypeId(), t.getLeaveTypeName(), t.getIsPaid());
    }
}
