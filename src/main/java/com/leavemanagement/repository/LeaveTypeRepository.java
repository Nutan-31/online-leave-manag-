package com.leavemanagement.repository;

import com.leavemanagement.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    boolean existsByLeaveTypeNameIgnoreCase(String leaveTypeName);
}
