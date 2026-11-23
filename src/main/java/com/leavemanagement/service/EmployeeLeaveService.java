package com.leavemanagement.service;

import com.leavemanagement.dto.*;
import com.leavemanagement.entity.*;
import com.leavemanagement.exception.BusinessRuleException;
import com.leavemanagement.exception.DuplicateResourceException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.repository.EmployeeLeaveRepository;
import com.leavemanagement.repository.EmployeeRepository;
import com.leavemanagement.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeLeaveService {

    // Default policy for General Paid Leave: 15 days per year
    private static final BigDecimal DEFAULT_ALLOCATED_DAYS = new BigDecimal("15.0");

    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTransactionService leaveTransactionService;

    /** Gives an employee a policy + opening balance (a CREDIT transaction) for one paid leave type. */
    @Transactional
    public EmployeeLeaveResponse createEmployeeLeave(EmployeeLeaveCreateRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + request.employeeId()));
        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id " + request.leaveTypeId()));

        // Rule: unpaid leave does not use a paid leave balance
        if (!leaveType.getIsPaid()) {
            throw new BusinessRuleException("'" + leaveType.getLeaveTypeName() + "' is unpaid leave and does not use a leave balance");
        }

        // Rule: unique employee + leave type
        if (employeeLeaveRepository.existsByEmployeeEmpIdAndLeaveTypeLeaveTypeId(employee.getEmpId(), leaveType.getLeaveTypeId())) {
            throw new DuplicateResourceException("Employee " + employee.getEmpId()
                    + " already has a leave policy for '" + leaveType.getLeaveTypeName() + "'");
        }

        BigDecimal allocated = request.allocatedDays() != null ? request.allocatedDays() : DEFAULT_ALLOCATED_DAYS;
        boolean carryAllowed = Boolean.TRUE.equals(request.carryForwardAllowed());
        BigDecimal carryLimit = carryAllowed ? request.carryForwardLimit() : null;

        validatePolicy(allocated, request.maximumBalance());

        EmployeeLeave el = new EmployeeLeave();
        el.setEmployee(employee);
        el.setLeaveType(leaveType);
        el.setAllocatedDays(allocated);
        el.setLeaveBalance(BigDecimal.ZERO);
        el.setDaysUsed(BigDecimal.ZERO);
        el.setRenewalFrequency(request.renewalFrequency() != null ? request.renewalFrequency() : RenewalFrequency.ANNUAL);
        el.setCarryForwardAllowed(carryAllowed);
        el.setCarryForwardLimit(carryLimit);
        el.setMaximumBalance(request.maximumBalance());
        el.setExcessLeaveAction(request.excessLeaveAction() != null ? request.excessLeaveAction() : ExcessLeaveAction.EXPIRE);
        el = employeeLeaveRepository.save(el);

        // Opening allocation: +allocated  ->  CREDIT row
        leaveTransactionService.applyChange(el, null, TransactionType.CREDIT, allocated);

        return EmployeeLeaveResponse.from(el);
    }

    /** Changes the policy only (rules for the next renewal). The current balance is not touched. */
    @Transactional
    public EmployeeLeaveResponse updatePolicy(Long employeeLeaveId, EmployeeLeavePolicyRequest request) {
        EmployeeLeave el = findEmployeeLeave(employeeLeaveId);

        validatePolicy(request.allocatedDays(), request.maximumBalance());

        boolean carryAllowed = request.carryForwardAllowed();
        el.setAllocatedDays(request.allocatedDays());
        el.setRenewalFrequency(request.renewalFrequency());
        el.setCarryForwardAllowed(carryAllowed);
        el.setCarryForwardLimit(carryAllowed ? request.carryForwardLimit() : null);
        el.setMaximumBalance(request.maximumBalance());
        el.setExcessLeaveAction(request.excessLeaveAction());

        return EmployeeLeaveResponse.from(employeeLeaveRepository.save(el));
    }

    /** Admin adjustment (+ or -). Recorded as an ADJUSTMENT transaction. Balance cannot go below 0 or above the maximum. */
    @Transactional
    public EmployeeLeaveResponse adjustBalance(Long employeeLeaveId, BalanceAdjustmentRequest request) {
        // Lock the row so nobody else changes the balance at the same time
        EmployeeLeave el = employeeLeaveRepository.findByIdForUpdate(employeeLeaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee leave record not found with id " + employeeLeaveId));

        BigDecimal days = request.days();
        if (days.signum() == 0) {
            throw new BusinessRuleException("Adjustment days cannot be 0");
        }

        BigDecimal newBalance = el.getLeaveBalance().add(days);
        if (el.getMaximumBalance() != null && newBalance.compareTo(el.getMaximumBalance()) > 0) {
            throw new BusinessRuleException("Adjustment would exceed the maximum balance of " + el.getMaximumBalance());
        }

        // (negative result is rejected inside applyChange)
        leaveTransactionService.applyChange(el, null, TransactionType.ADJUSTMENT, days);
        return EmployeeLeaveResponse.from(el);
    }

    /**
     * Start a new leave period (what happens at year end). Steps, all in ONE transaction:
     *  1. Work out how much of the old balance is carried forward (if allowed, up to carryForwardLimit).
     *  2. The rest is removed as EXPIRY or ENCASHMENT (excessLeaveAction).
     *  3. A CARRY_FORWARD marker row is written for the carried days.
     *  4. allocatedDays are added as CREDIT.
     *  5. If the balance is now above maximumBalance, the surplus is removed as EXPIRY / ENCASHMENT.
     *  6. daysUsed restarts at 0.
     * This is triggered manually by the admin (there is no scheduler in this project yet).
     */
    @Transactional
    public EmployeeLeaveResponse renew(Long employeeLeaveId) {
        EmployeeLeave el = employeeLeaveRepository.findByIdForUpdate(employeeLeaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee leave record not found with id " + employeeLeaveId));

        if (el.getRenewalFrequency() == RenewalFrequency.NONE) {
            throw new BusinessRuleException("This leave policy has renewalFrequency NONE and cannot be renewed");
        }

        TransactionType excessType = el.getExcessLeaveAction() == ExcessLeaveAction.ENCASH
                ? TransactionType.ENCASHMENT : TransactionType.EXPIRY;

        BigDecimal oldBalance = el.getLeaveBalance();

        // 1 + 2: carried vs excess
        BigDecimal carried = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(el.getCarryForwardAllowed())) {
            carried = el.getCarryForwardLimit() == null ? oldBalance : oldBalance.min(el.getCarryForwardLimit());
        }
        BigDecimal excess = oldBalance.subtract(carried);

        if (excess.signum() > 0) {
            leaveTransactionService.applyChange(el, null, excessType, excess.negate());
        }
        // 3
        if (carried.signum() > 0) {
            leaveTransactionService.recordCarryForward(el, carried);
        }
        // 4
        leaveTransactionService.applyChange(el, null, TransactionType.CREDIT, el.getAllocatedDays());

        // 5
        if (el.getMaximumBalance() != null && el.getLeaveBalance().compareTo(el.getMaximumBalance()) > 0) {
            BigDecimal surplus = el.getLeaveBalance().subtract(el.getMaximumBalance());
            leaveTransactionService.applyChange(el, null, excessType, surplus.negate());
        }

        // 6
        el.setDaysUsed(BigDecimal.ZERO);
        return EmployeeLeaveResponse.from(employeeLeaveRepository.save(el));
    }

    public List<EmployeeLeaveResponse> getAll() {
        return employeeLeaveRepository.findAll().stream().map(EmployeeLeaveResponse::from).toList();
    }

    public List<EmployeeLeaveResponse> getByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id " + employeeId);
        }
        return employeeLeaveRepository.findByEmployeeEmpId(employeeId).stream().map(EmployeeLeaveResponse::from).toList();
    }

    public EmployeeLeaveResponse getById(Long employeeLeaveId) {
        return EmployeeLeaveResponse.from(findEmployeeLeave(employeeLeaveId));
    }

    /** The ledger (all LeaveTransaction rows) for this employee + leave type, oldest first. */
    public List<LeaveTransactionResponse> getTransactions(Long employeeLeaveId) {
        EmployeeLeave el = findEmployeeLeave(employeeLeaveId);
        return leaveTransactionService.getHistory(el.getEmployee().getEmpId(), el.getLeaveType().getLeaveTypeId());
    }

    private EmployeeLeave findEmployeeLeave(Long id) {
        return employeeLeaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee leave record not found with id " + id));
    }

    private void validatePolicy(BigDecimal allocatedDays, BigDecimal maximumBalance) {
        if (maximumBalance != null && allocatedDays.compareTo(maximumBalance) > 0) {
            throw new BusinessRuleException("allocatedDays (" + allocatedDays + ") cannot be more than maximumBalance (" + maximumBalance + ")");
        }
    }
}
