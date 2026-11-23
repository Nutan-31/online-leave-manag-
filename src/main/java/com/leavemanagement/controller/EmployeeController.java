package com.leavemanagement.controller;

import com.leavemanagement.dto.EmployeeRequest;
import com.leavemanagement.dto.EmployeeResponse;
import com.leavemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(@Valid @RequestBody EmployeeRequest request,
                                           @RequestParam Long departmentId) {
        return employeeService.createEmployee(request, departmentId);
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    // Assign the REPORTING manager
    @PutMapping("/{employeeId}/manager/{managerId}")
    public EmployeeResponse assignManager(@PathVariable Long employeeId,
                                          @PathVariable Long managerId) {
        return employeeService.assignManager(employeeId, managerId);
    }

    // Employees who report to this manager
    @GetMapping("/manager/{managerId}")
    public List<EmployeeResponse> getEmployeesByManager(@PathVariable Long managerId) {
        return employeeService.getEmployeesByManager(managerId);
    }
}
