package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveTypeId;

    @Column(nullable = false, unique = true)
    private String leaveTypeName;

    /** true = paid leave (uses a balance). false = unpaid leave (no balance). */
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;
}
