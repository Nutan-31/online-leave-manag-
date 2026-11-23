package com.leavemanagement.repository;

import com.leavemanagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByDepartmentNameIgnoreCase(String departmentName);

    // Which department (if any) does this employee manage as DEPARTMENT manager?
    Optional<Department> findByDepartmentManagerEmpId(Long empId);
}
