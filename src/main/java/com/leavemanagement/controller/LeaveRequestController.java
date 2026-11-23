package com.leavemanagement.controller;

import com.leavemanagement.dto.LeaveRequestCreateRequest;
import com.leavemanagement.dto.LeaveRequestResponse;
import com.leavemanagement.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    // ---------- apply ----------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestResponse apply(@Valid @RequestBody LeaveRequestCreateRequest request) {
        return leaveRequestService.applyForLeave(request);
    }

    // ---------- read ----------
    @GetMapping
    public List<LeaveRequestResponse> getAll() {
        return leaveRequestService.getAll();
    }

    @GetMapping("/{id}")
    public LeaveRequestResponse getById(@PathVariable Long id) {
        return leaveRequestService.getById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public List<LeaveRequestResponse> getByEmployee(@PathVariable Long employeeId) {
        return leaveRequestService.getByEmployee(employeeId);
    }

    // Manager inbox: requests (and cancellation requests) from people who report to this manager
    @GetMapping("/manager/{managerId}/pending")
    public List<LeaveRequestResponse> getAwaitingDecision(@PathVariable Long managerId) {
        return leaveRequestService.getAwaitingDecision(managerId);
    }

    // ---------- manager decisions (approverId = the employee id of the person deciding; JWT replaces this in Step 10) ----------
    @PutMapping("/{id}/approve")
    public LeaveRequestResponse approve(@PathVariable Long id, @RequestParam Long approverId) {
        return leaveRequestService.approve(id, approverId);
    }

    @PutMapping("/{id}/reject")
    public LeaveRequestResponse reject(@PathVariable Long id, @RequestParam Long approverId) {
        return leaveRequestService.reject(id, approverId);
    }

    // ---------- cancellation ----------
    @PutMapping("/{id}/cancel")
    public LeaveRequestResponse cancelPending(@PathVariable Long id) {
        return leaveRequestService.cancelPending(id);
    }

    @PutMapping("/{id}/request-cancellation")
    public LeaveRequestResponse requestCancellation(@PathVariable Long id) {
        return leaveRequestService.requestCancellation(id);
    }

    @PutMapping("/{id}/approve-cancellation")
    public LeaveRequestResponse approveCancellation(@PathVariable Long id, @RequestParam Long approverId) {
        return leaveRequestService.approveCancellation(id, approverId);
    }

    @PutMapping("/{id}/reject-cancellation")
    public LeaveRequestResponse rejectCancellation(@PathVariable Long id, @RequestParam Long approverId) {
        return leaveRequestService.rejectCancellation(id, approverId);
    }
}
