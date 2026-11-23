package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @Column(nullable = false, unique = true)
    private String departmentName;

    /**
     * DEPARTMENT manager (administrative head of the department).
     * NOT the same as an employee's reporting manager and NOT automatically a leave approver.
     *
     * unique = true  ->  one employee can manage at most one department.
     * Being a single column, a department also has at most one manager.
     * Nullable: a department is created first, the manager is assigned later.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_manager_id", unique = true)
    private Employee departmentManager;
}
