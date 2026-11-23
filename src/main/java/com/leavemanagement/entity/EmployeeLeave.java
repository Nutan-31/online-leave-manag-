package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One row = the leave policy AND the current balance of ONE employee for ONE (paid) leave type.
 */
@Entity
@Table(
        name = "employee_leave",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class EmployeeLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeLeaveId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    /** Days granted each renewal period (e.g. 15 per year). */
    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal allocatedDays;

    /** Current balance. Must never be negative. Changed ONLY through LeaveTransactionService. */
    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal leaveBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RenewalFrequency renewalFrequency;

    @Column(nullable = false)
    private Boolean carryForwardAllowed;

    /** Max days carried to the next period. Null = no limit (when carry forward is allowed). */
    @Column(precision = 6, scale = 1)
    private BigDecimal carryForwardLimit;

    /** Balance can never be kept above this. Null = no cap. */
    @Column(precision = 6, scale = 1)
    private BigDecimal maximumBalance;

    /** What happens to days that cannot be carried / exceed the maximum: EXPIRE or ENCASH. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExcessLeaveAction excessLeaveAction;

    /** Days used in the current period. */
    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal daysUsed;
}
