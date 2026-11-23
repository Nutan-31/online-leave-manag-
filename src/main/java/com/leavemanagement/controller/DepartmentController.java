package com.leavemanagement.controller;

import com.leavemanagement.dto.DepartmentRequest;
import com.leavemanagement.dto.DepartmentResponse;
import com.leavemanagement.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return departmentService.createDepartment(request);
    }

    @GetMapping
    public List<DepartmentResponse> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    // Assign the DEPARTMENT manager
    @PutMapping("/{departmentId}/manager/{employeeId}")
    public DepartmentResponse assignDepartmentManager(@PathVariable Long departmentId,
                                                      @PathVariable Long employeeId) {
        return departmentService.assignDepartmentManager(departmentId, employeeId);
    }
}
