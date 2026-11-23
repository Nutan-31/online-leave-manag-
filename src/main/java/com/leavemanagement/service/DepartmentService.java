package com.leavemanagement.service;

import com.leavemanagement.dto.DepartmentRequest;
import com.leavemanagement.dto.DepartmentResponse;
import com.leavemanagement.entity.Department;
import com.leavemanagement.entity.Employee;
import com.leavemanagement.exception.BusinessRuleException;
import com.leavemanagement.exception.DuplicateResourceException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.repository.DepartmentRepository;
import com.leavemanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    /** A department is created WITHOUT a manager. Employees come first, the manager is assigned afterwards. */
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String name = request.departmentName().trim();

        if (departmentRepository.existsByDepartmentNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Department '" + name + "' already exists");
        }

        Department department = new Department();
        department.setDepartmentName(name);
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream().map(DepartmentResponse::from).toList();
    }

    public DepartmentResponse getDepartmentById(Long departmentId) {
        return DepartmentResponse.from(findDepartment(departmentId));
    }

    /**
     * Assign the DEPARTMENT manager.
     * Rules: the employee must belong to this department, and an employee can manage at most one department.
     * (This does NOT make the person the leave approver - approval follows the REPORTING manager.)
     */
    @Transactional
    public DepartmentResponse assignDepartmentManager(Long departmentId, Long employeeId) {
        Department department = findDepartment(departmentId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + employeeId));

        // Rule: department manager must belong to that department
        if (!employee.getDepartment().getDepartmentId().equals(departmentId)) {
            throw new BusinessRuleException("Employee " + employeeId
                    + " belongs to department '" + employee.getDepartment().getDepartmentName()
                    + "', so they cannot manage department '" + department.getDepartmentName() + "'");
        }

        // Rule: one employee manages at most one department
        Optional<Department> alreadyManaged = departmentRepository.findByDepartmentManagerEmpId(employeeId);
        if (alreadyManaged.isPresent() && !alreadyManaged.get().getDepartmentId().equals(departmentId)) {
            throw new BusinessRuleException("Employee " + employeeId + " already manages department '"
                    + alreadyManaged.get().getDepartmentName() + "'");
        }

        // Rule: a department has one manager -> assigning simply replaces the previous one
        department.setDepartmentManager(employee);
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + departmentId));
    }
}
