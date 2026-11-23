package com.leavemanagement.controller;

import com.leavemanagement.dto.*;
import com.leavemanagement.service.EmployeeLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-leaves")
@RequiredArgsConstructor
public class EmployeeLeaveController {

    private final EmployeeLeaveService employeeLeaveService;

    // Give an employee a leave policy + opening balance
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeLeaveResponse create(@Valid @RequestBody EmployeeLeaveCreateRequest request) {
        return employeeLeaveService.createEmployeeLeave(request);
    }

    @GetMapping
    public List<EmployeeLeaveResponse> getAll() {
        return employeeLeaveService.getAll();
    }

    @GetMapping("/{id}")
    public EmployeeLeaveResponse getById(@PathVariable Long id) {
        return employeeLeaveService.getById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public List<EmployeeLeaveResponse> getByEmployee(@PathVariable Long employeeId) {
        return employeeLeaveService.getByEmployee(employeeId);
    }

    // Change policy rules (does not touch the balance)
    @PutMapping("/{id}/policy")
    public EmployeeLeaveResponse updatePolicy(@PathVariable Long id,
                                              @Valid @RequestBody EmployeeLeavePolicyRequest request) {
        return employeeLeaveService.updatePolicy(id, request);
    }

    // Admin balance adjustment (+ or -)
    @PostMapping("/{id}/adjust")
    public EmployeeLeaveResponse adjust(@PathVariable Long id,
                                        @Valid @RequestBody BalanceAdjustmentRequest request) {
        return employeeLeaveService.adjustBalance(id, request);
    }

    // Start a new leave period (carry forward / expiry / encashment / new allocation)
    @PostMapping("/{id}/renew")
    public EmployeeLeaveResponse renew(@PathVariable Long id) {
        return employeeLeaveService.renew(id);
    }

    // Ledger (read only)
    @GetMapping("/{id}/transactions")
    public List<LeaveTransactionResponse> getTransactions(@PathVariable Long id) {
        return employeeLeaveService.getTransactions(id);
    }
}
