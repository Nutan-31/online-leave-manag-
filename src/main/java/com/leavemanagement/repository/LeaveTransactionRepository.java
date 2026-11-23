package com.leavemanagement.repository;

import com.leavemanagement.entity.LeaveTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveTransactionRepository extends JpaRepository<LeaveTransaction, Long> {

    List<LeaveTransaction> findByEmployeeEmpIdAndLeaveTypeLeaveTypeIdOrderByTransactionIdAsc(Long empId, Long leaveTypeId);
}
