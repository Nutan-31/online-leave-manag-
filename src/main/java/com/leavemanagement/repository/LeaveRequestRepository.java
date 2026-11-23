package com.leavemanagement.repository;

import com.leavemanagement.entity.LeaveRequest;
import com.leavemanagement.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findAllByOrderByAppliedAtDesc();

    List<LeaveRequest> findByEmployeeEmpIdOrderByAppliedAtDesc(Long empId);

    // Requests of the people who report to this manager, filtered by status
    List<LeaveRequest> findByEmployeeManagerEmpIdAndStatusInOrderByAppliedAtAsc(Long managerId, Collection<LeaveStatus> statuses);

    // Does this employee already have a request (with one of the given statuses) touching these dates?
    @Query("""
            select count(r) > 0 from LeaveRequest r
            where r.employee.empId = :empId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    boolean existsOverlapping(@Param("empId") Long empId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("statuses") Collection<LeaveStatus> statuses);
}
