package com.leavemanagement.controller;

import com.leavemanagement.dto.LeaveTypeRequest;
import com.leavemanagement.dto.LeaveTypeResponse;
import com.leavemanagement.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveTypeResponse createLeaveType(@Valid @RequestBody LeaveTypeRequest request) {
        return leaveTypeService.createLeaveType(request);
    }

    @GetMapping
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeService.getAllLeaveTypes();
    }

    @GetMapping("/{id}")
    public LeaveTypeResponse getLeaveTypeById(@PathVariable Long id) {
        return leaveTypeService.getLeaveTypeById(id);
    }
}
