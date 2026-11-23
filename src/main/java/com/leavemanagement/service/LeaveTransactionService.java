package com.leavemanagement.service;

import com.leavemanagement.dto.LeaveTransactionResponse;
import com.leavemanagement.entity.EmployeeLeave;
import com.leavemanagement.entity.LeaveRequest;
import com.leavemanagement.entity.LeaveTransaction;
import com.leavemanagement.entity.TransactionType;
import com.leavemanagement.exception.ConflictException;
import com.leavemanagement.repository.EmployeeLeaveRepository;
import com.leavemanagement.repository.LeaveTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * THE ONLY PLACE where a leave balance is allowed to change.
 * Every change updates EmployeeLeave AND writes one LeaveTransaction row, together.
 *
 * Propagation.MANDATORY means: this must be called from inside a bigger @Transactional method
 * (approve leave, cancel leave, renew...). That way "change balance + write ledger row + update request"
 * all succeed or all roll back as one unit.
 */
@Service
@RequiredArgsConstructor
public class LeaveTransactionService {

    private final LeaveTransactionRepository leaveTransactionRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;

    /**
     * @param days signed amount: positive adds, negative removes
     * @param request the leave request behind the change (DEBIT / RESTORATION), otherwise null
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LeaveTransaction applyChange(EmployeeLeave employeeLeave, LeaveRequest request,
                                        TransactionType type, BigDecimal days) {
        checkSign(type, days);

        BigDecimal newBalance = employeeLeave.getLeaveBalance().add(days);

        // Rule: paid leave balance can never become negative
        if (newBalance.signum() < 0) {
            throw new ConflictException("Insufficient leave balance. Current balance: "
                    + employeeLeave.getLeaveBalance() + ", requested change: " + days);
        }

        employeeLeave.setLeaveBalance(newBalance);

        if (type == TransactionType.DEBIT) {
            employeeLeave.setDaysUsed(employeeLeave.getDaysUsed().add(days.abs()));
        } else if (type == TransactionType.RESTORATION) {
            BigDecimal used = employeeLeave.getDaysUsed().subtract(days.abs());
            employeeLeave.setDaysUsed(used.signum() < 0 ? BigDecimal.ZERO : used);
        }
        employeeLeaveRepository.save(employeeLeave);

        return writeRow(employeeLeave, request, type, days, newBalance);
    }

    /**
     * CARRY_FORWARD is a marker row: the carried days simply STAY in the balance, so the balance does
     * not change here. The row records how many days were carried into the new period.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LeaveTransaction recordCarryForward(EmployeeLeave employeeLeave, BigDecimal carriedDays) {
        return writeRow(employeeLeave, null, TransactionType.CARRY_FORWARD, carriedDays, employeeLeave.getLeaveBalance());
    }

    @Transactional(readOnly = true)
    public List<LeaveTransactionResponse> getHistory(Long employeeId, Long leaveTypeId) {
        return leaveTransactionRepository
                .findByEmployeeEmpIdAndLeaveTypeLeaveTypeIdOrderByTransactionIdAsc(employeeId, leaveTypeId)
                .stream().map(LeaveTransactionResponse::from).toList();
    }

    private LeaveTransaction writeRow(EmployeeLeave el, LeaveRequest request, TransactionType type,
                                      BigDecimal days, BigDecimal balanceAfter) {
        LeaveTransaction tx = new LeaveTransaction();
        tx.setEmployee(el.getEmployee());
        tx.setLeaveType(el.getLeaveType());
        tx.setRequest(request);
        tx.setTransactionType(type);
        tx.setDays(days);
        tx.setBalanceAfter(balanceAfter);
        tx.setTransactionDate(LocalDateTime.now());
        return leaveTransactionRepository.save(tx);
    }

    // Programming-error guard: e.g. a DEBIT must be negative
    private void checkSign(TransactionType type, BigDecimal days) {
        boolean ok = switch (type) {
            case CREDIT, CARRY_FORWARD, RESTORATION -> days.signum() > 0;
            case DEBIT, EXPIRY, ENCASHMENT -> days.signum() < 0;
            case ADJUSTMENT -> days.signum() != 0;
        };
        if (!ok) {
            throw new IllegalArgumentException("Invalid amount " + days + " for transaction type " + type);
        }
    }
}
