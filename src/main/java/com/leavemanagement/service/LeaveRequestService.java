package com.leavemanagement.service;

import com.leavemanagement.dto.LeaveRequestCreateRequest;
import com.leavemanagement.dto.LeaveRequestResponse;
import com.leavemanagement.entity.*;
import com.leavemanagement.exception.BusinessRuleException;
import com.leavemanagement.exception.ConflictException;
import com.leavemanagement.exception.NotAuthorizedException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveRequestService {

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    // A request in one of these statuses still "holds" the dates, so new requests may not overlap it
    private static final List<LeaveStatus> DATE_HOLDING_STATUSES =
            List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED, LeaveStatus.CANCELLATION_PENDING);

    // What a manager still has to act on
    private static final List<LeaveStatus> NEEDS_DECISION_STATUSES =
            List.of(LeaveStatus.PENDING, LeaveStatus.CANCELLATION_PENDING);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final UserAccountRepository userAccountRepository;
    private final LeaveTransactionService leaveTransactionService;
    private final WorkingDayService workingDayService;

    // =====================================================================
    //  APPLY
    // =====================================================================

    /** Employee (or manager) applies for leave. Status starts as PENDING. No balance is deducted yet. */
    @Transactional
    public LeaveRequestResponse applyForLeave(LeaveRequestCreateRequest request) {
        Employee employee = findEmployee(request.employeeId());
        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id " + request.leaveTypeId()));

        // Rule: inactive accounts cannot be used
        userAccountRepository.findByEmployeeEmpId(employee.getEmpId()).ifPresent(account -> {
            if (account.getAccountStatus() == AccountStatus.INACTIVE) {
                throw new BusinessRuleException("This employee's account is inactive and cannot apply for leave");
            }
        });

        // Rule: valid dates
        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessRuleException("startDate cannot be after endDate");
        }

        // Rule: weekends (and public holidays) are excluded
        BigDecimal totalDays = calculateTotalDays(request);

        // Rule: no overlapping pending/approved leave
        if (leaveRequestRepository.existsOverlapping(employee.getEmpId(), request.startDate(), request.endDate(), DATE_HOLDING_STATUSES)) {
            throw new ConflictException("You already have a pending or approved leave request overlapping these dates");
        }

        // Rule: paid leave needs enough balance (checked again at approval time)
        if (leaveType.getIsPaid()) {
            EmployeeLeave balance = employeeLeaveRepository
                    .findByEmployeeEmpIdAndLeaveTypeLeaveTypeId(employee.getEmpId(), leaveType.getLeaveTypeId())
                    .orElseThrow(() -> new BusinessRuleException("Employee " + employee.getEmpId()
                            + " has no leave policy/balance for '" + leaveType.getLeaveTypeName() + "'"));

            if (balance.getLeaveBalance().compareTo(totalDays) < 0) {
                throw new ConflictException("Insufficient leave balance. Available: "
                        + balance.getLeaveBalance() + ", requested: " + totalDays);
            }
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setTotalDays(totalDays);
        leaveRequest.setReason(request.reason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setAppliedAt(LocalDateTime.now());

        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    // =====================================================================
    //  APPROVE / REJECT
    // =====================================================================

    /**
     * Approve leave. ONE transaction: if anything below fails, everything rolls back.
     *   status -> APPROVED
     *   paid leave: balance is deducted (re-checked now) + a DEBIT transaction is written
     */
    @Transactional
    public LeaveRequestResponse approve(Long requestId, Long approverId) {
        LeaveRequest leaveRequest = findRequest(requestId);
        requireStatus(leaveRequest, LeaveStatus.PENDING, "Only PENDING requests can be approved");
        Employee approver = findEmployee(approverId);
        checkCanDecide(leaveRequest, approver);

        if (leaveRequest.getLeaveType().getIsPaid()) {
            // Lock + re-check balance. If it is too low, applyChange throws and nothing is saved.
            EmployeeLeave balance = employeeLeaveRepository
                    .findForUpdate(leaveRequest.getEmployee().getEmpId(), leaveRequest.getLeaveType().getLeaveTypeId())
                    .orElseThrow(() -> new BusinessRuleException("Employee has no leave policy/balance for '"
                            + leaveRequest.getLeaveType().getLeaveTypeName() + "'"));

            leaveTransactionService.applyChange(balance, leaveRequest, TransactionType.DEBIT, leaveRequest.getTotalDays().negate());
        }
        // Unpaid leave: no balance involved, so no transaction

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approver);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    /** Reject leave. No balance change. approvedBy/approvedAt store who/when decided. */
    @Transactional
    public LeaveRequestResponse reject(Long requestId, Long approverId) {
        LeaveRequest leaveRequest = findRequest(requestId);
        requireStatus(leaveRequest, LeaveStatus.PENDING, "Only PENDING requests can be rejected");
        Employee approver = findEmployee(approverId);
        checkCanDecide(leaveRequest, approver);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(approver);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    // =====================================================================
    //  CANCELLATION
    // =====================================================================

    /** A PENDING request can simply be cancelled by the employee. No balance was deducted, so nothing to restore. */
    @Transactional
    public LeaveRequestResponse cancelPending(Long requestId) {
        LeaveRequest leaveRequest = findRequest(requestId);
        requireStatus(leaveRequest, LeaveStatus.PENDING, "Only PENDING requests can be cancelled directly. "
                + "For an APPROVED request use request-cancellation.");

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest.setCancelledAt(LocalDateTime.now());
        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    /** An APPROVED request cannot just disappear: the employee asks, the manager must approve. */
    @Transactional
    public LeaveRequestResponse requestCancellation(Long requestId) {
        LeaveRequest leaveRequest = findRequest(requestId);
        requireStatus(leaveRequest, LeaveStatus.APPROVED, "Only APPROVED requests can be sent for cancellation approval");

        leaveRequest.setStatus(LeaveStatus.CANCELLATION_PENDING);
        leaveRequest.setCancellationRequestedAt(LocalDateTime.now());
        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    /**
     * Manager approves the cancellation. ONE transaction:
     *   status -> CANCELLED
     *   paid leave: balance is restored + a RESTORATION transaction is written
     */
    @Transactional
    public LeaveRequestResponse approveCancellation(Long requestId, Long approverId) {
        LeaveRequest leaveRequest = findRequest(requestId);
        requireStatus(leaveRequest, LeaveStatus.CANCELLATION_PENDING, "Only CANCELLATION_PENDING requests can be approved for cancellation");
        Employee approver = findEmployee(approverId);
        checkCanDecide(leaveRequest, approver);

        if (leaveRequest.getLeaveType().getIsPaid()) {
            EmployeeLeave balance = employeeLeaveRepository
                    .findForUpdate(leaveRequest.getEmployee().getEmpId(), leaveRequest.getLeaveType().getLeaveTypeId())
                    .orElseThrow(() -> new BusinessRuleException("Employee has no leave policy/balance for '"
                            + leaveRequest.getLeaveType().getLeaveTypeName() + "'"));

            leaveTransactionService.applyChange(balance, leaveRequest, TransactionType.RESTORATION, leaveRequest.getTotalDays());
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest.setCancellationApprovedBy(approver);
        leaveRequest.setCancelledAt(LocalDateTime.now());
        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    /** Manager refuses the cancellation: the leave stays APPROVED (and stays deducted). */
    @Transactional
    public LeaveRequestResponse rejectCancellation(Long requestId, Long approverId) {
        LeaveRequest leaveRequest = findRequest(requestId);
        requireStatus(leaveRequest, LeaveStatus.CANCELLATION_PENDING, "Only CANCELLATION_PENDING requests can be rejected for cancellation");
        Employee approver = findEmployee(approverId);
        checkCanDecide(leaveRequest, approver);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setCancellationRequestedAt(null);
        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    // =====================================================================
    //  READ
    // =====================================================================

    public List<LeaveRequestResponse> getAll() {
        return leaveRequestRepository.findAllByOrderByAppliedAtDesc().stream().map(LeaveRequestResponse::from).toList();
    }

    public LeaveRequestResponse getById(Long requestId) {
        return LeaveRequestResponse.from(findRequest(requestId));
    }

    public List<LeaveRequestResponse> getByEmployee(Long employeeId) {
        findEmployee(employeeId); // 404 if the employee does not exist
        return leaveRequestRepository.findByEmployeeEmpIdOrderByAppliedAtDesc(employeeId)
                .stream().map(LeaveRequestResponse::from).toList();
    }

    /** The manager's inbox: PENDING requests and CANCELLATION_PENDING requests of the people who report to them. */
    public List<LeaveRequestResponse> getAwaitingDecision(Long managerId) {
        findEmployee(managerId);
        return leaveRequestRepository.findByEmployeeManagerEmpIdAndStatusInOrderByAppliedAtAsc(managerId, NEEDS_DECISION_STATUSES)
                .stream().map(LeaveRequestResponse::from).toList();
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    /**
     * Working days = days between start and end minus weekends and public holidays.
     * If the caller sends totalDays (for half days) it must be a multiple of 0.5 and may be at most
     * 1 day lower than the working days (i.e. at most one half-day at each end of the range).
     */
    private BigDecimal calculateTotalDays(LeaveRequestCreateRequest request) {
        int workingDays = workingDayService.countWorkingDays(request.startDate(), request.endDate());
        if (workingDays == 0) {
            throw new BusinessRuleException("The selected dates contain only weekends or public holidays");
        }

        BigDecimal maxDays = BigDecimal.valueOf(workingDays).setScale(1);
        if (request.totalDays() == null) {
            return maxDays;
        }

        BigDecimal totalDays = request.totalDays();
        if (totalDays.remainder(HALF_DAY).signum() != 0) {
            throw new BusinessRuleException("totalDays must be in steps of 0.5 (0.5, 1.0, 1.5 ...)");
        }

        BigDecimal minDays = HALF_DAY.max(maxDays.subtract(BigDecimal.ONE));
        if (totalDays.compareTo(maxDays) > 0 || totalDays.compareTo(minDays) < 0) {
            throw new BusinessRuleException("For these dates there are " + maxDays + " working day(s), so totalDays must be between "
                    + minDays + " and " + maxDays);
        }
        return totalDays.setScale(1);
    }

    /**
     * Who may approve/reject?
     *  - never the applicant themselves
     *  - the approver's own account must not be INACTIVE
     *  - if the applicant has a REPORTING manager -> only that manager (NOT the department manager)
     *  - if the applicant has no reporting manager -> only an employee with an ACTIVE ADMIN account
     *    (assumption - see README, "Open design questions")
     */
    private void checkCanDecide(LeaveRequest leaveRequest, Employee approver) {
        Employee applicant = leaveRequest.getEmployee();

        if (applicant.getEmpId().equals(approver.getEmpId())) {
            throw new NotAuthorizedException("You cannot approve or reject your own request");
        }

        Optional<UserAccount> approverAccount = userAccountRepository.findByEmployeeEmpId(approver.getEmpId());
        if (approverAccount.isPresent() && approverAccount.get().getAccountStatus() == AccountStatus.INACTIVE) {
            throw new NotAuthorizedException("The approver's account is inactive");
        }

        Employee reportingManager = applicant.getManager();
        if (reportingManager != null) {
            if (!reportingManager.getEmpId().equals(approver.getEmpId())) {
                throw new NotAuthorizedException("Only the reporting manager (employee id "
                        + reportingManager.getEmpId() + ") can decide this request");
            }
        } else {
            boolean activeAdmin = approverAccount.isPresent()
                    && approverAccount.get().getRole() == Role.ADMIN
                    && approverAccount.get().getAccountStatus() == AccountStatus.ACTIVE;
            if (!activeAdmin) {
                throw new NotAuthorizedException("This employee has no reporting manager, so only an admin can decide this request");
            }
        }
    }

    private void requireStatus(LeaveRequest leaveRequest, LeaveStatus expected, String message) {
        if (leaveRequest.getStatus() != expected) {
            throw new ConflictException(message + " (current status: " + leaveRequest.getStatus() + ")");
        }
    }

    private LeaveRequest findRequest(Long requestId) {
        return leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id " + requestId));
    }

    private Employee findEmployee(Long empId) {
        return employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + empId));
    }
}
