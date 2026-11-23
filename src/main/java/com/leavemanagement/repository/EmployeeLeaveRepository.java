package com.leavemanagement.repository;

import com.leavemanagement.entity.EmployeeLeave;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long> {

    boolean existsByEmployeeEmpIdAndLeaveTypeLeaveTypeId(Long empId, Long leaveTypeId);

    Optional<EmployeeLeave> findByEmployeeEmpIdAndLeaveTypeLeaveTypeId(Long empId, Long leaveTypeId);

    List<EmployeeLeave> findByEmployeeEmpId(Long empId);

    // The two methods below LOCK the row until the transaction ends, so two people
    // approving leave at the same moment cannot both spend the same balance.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select el from EmployeeLeave el where el.employee.empId = :empId and el.leaveType.leaveTypeId = :leaveTypeId")
    Optional<EmployeeLeave> findForUpdate(@Param("empId") Long empId, @Param("leaveTypeId") Long leaveTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select el from EmployeeLeave el where el.employeeLeaveId = :id")
    Optional<EmployeeLeave> findByIdForUpdate(@Param("id") Long id);
}
