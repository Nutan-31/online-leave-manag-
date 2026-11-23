package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The ledger. Every authorised change of a leave balance creates exactly one row here.
 * Rows are never updated or deleted.
 */
@Entity
@Table(name = "leave_transactions")
@Getter
@Setter
@NoArgsConstructor
public class LeaveTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    /** The leave request that caused it (only for DEBIT / RESTORATION), otherwise null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private LeaveRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    /** Signed: positive adds to the balance, negative removes. */
    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal days;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private LocalDateTime transactionDate;
}
