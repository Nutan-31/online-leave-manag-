package com.leavemanagement.repository;

import com.leavemanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    // Employees whose REPORTING manager is the given employee
    List<Employee> findByManagerEmpId(Long managerId);
}
