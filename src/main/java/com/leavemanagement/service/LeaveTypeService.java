package com.leavemanagement.service;

import com.leavemanagement.dto.LeaveTypeRequest;
import com.leavemanagement.dto.LeaveTypeResponse;
import com.leavemanagement.entity.LeaveType;
import com.leavemanagement.exception.DuplicateResourceException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        String name = request.leaveTypeName().trim();

        if (leaveTypeRepository.existsByLeaveTypeNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Leave type '" + name + "' already exists");
        }

        LeaveType leaveType = new LeaveType();
        leaveType.setLeaveTypeName(name);
        leaveType.setIsPaid(request.isPaid());
        return LeaveTypeResponse.from(leaveTypeRepository.save(leaveType));
    }

    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream().map(LeaveTypeResponse::from).toList();
    }

    public LeaveTypeResponse getLeaveTypeById(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id " + id));
        return LeaveTypeResponse.from(leaveType);
    }
}
