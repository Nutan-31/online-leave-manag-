package com.leavemanagement.service;

import com.leavemanagement.dto.EmployeeRequest;
import com.leavemanagement.dto.EmployeeResponse;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request, Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + departmentId));

        String email = request.email().trim().toLowerCase();
        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An employee with email '" + email + "' already exists");
        }

        Employee employee = new Employee();
        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setDesignation(request.designation().trim());
        employee.setDateOfJoining(request.dateOfJoining());
        employee.setDepartment(department);
        // reporting manager stays null until assignManager() is called

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    /** Assign the REPORTING manager (the person who approves this employee's leave). */
    @Transactional
    public EmployeeResponse assignManager(Long employeeId, Long managerId) {
        Employee employee = findEmployee(employeeId);
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id " + managerId));

        // Rule: employee cannot be their own manager
        if (employeeId.equals(managerId)) {
            throw new BusinessRuleException("An employee cannot be their own reporting manager");
        }

        // Rule: reporting manager must belong to the same department
        if (!employee.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new BusinessRuleException("The reporting manager must belong to the same department as the employee");
        }

        // Extra safety: no loops (A reports to B, B reports to A, ...)
        Set<Long> visited = new HashSet<>();
        Employee current = manager;
        while (current != null && visited.add(current.getEmpId())) {
            if (current.getEmpId().equals(employeeId)) {
                throw new BusinessRuleException("This would create a reporting loop: the chosen manager already reports (directly or indirectly) to this employee");
            }
            current = current.getManager();
        }

        employee.setManager(manager);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public List<EmployeeResponse> getEmployeesByManager(Long managerId) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager not found with id " + managerId);
        }
        return employeeRepository.findByManagerEmpId(managerId).stream().map(EmployeeResponse::from).toList();
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream().map(EmployeeResponse::from).toList();
    }

    public EmployeeResponse getEmployeeById(Long id) {
        return EmployeeResponse.from(findEmployee(id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }
}
