package com.leavemanagement.dto;

import com.leavemanagement.entity.LeaveTransaction;
import com.leavemanagement.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeaveTransactionResponse(
        Long transactionId,
        Long employeeId,
        Long leaveTypeId,
        String leaveTypeName,
        Long requestId,
        TransactionType transactionType,
        BigDecimal days,
        BigDecimal balanceAfter,
        LocalDateTime transactionDate
) {
    public static LeaveTransactionResponse from(LeaveTransaction t) {
        return new LeaveTransactionResponse(
                t.getTransactionId(),
                t.getEmployee().getEmpId(),
                t.getLeaveType().getLeaveTypeId(),
                t.getLeaveType().getLeaveTypeName(),
                t.getRequest() == null ? null : t.getRequest().getRequestId(),
                t.getTransactionType(),
                t.getDays(),
                t.getBalanceAfter(),
                t.getTransactionDate());
    }
}
