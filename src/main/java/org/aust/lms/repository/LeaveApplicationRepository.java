package org.aust.lms.repository;

import org.aust.lms.dto.ApplicantLeaveListResponse;
import org.aust.lms.entity.LeaveApplication;
import org.aust.lms.enums.LeaveActionRole;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    @Query("""
        SELECT a
        FROM LeaveApplication a
        JOIN FETCH a.employee e
        JOIN FETCH e.designation d
        JOIN FETCH a.leaveType lt
        WHERE a.id = :applicationId
        """
    )
    Optional<LeaveApplication> findDetailsById(@Param("applicationId") Long applicationId);

    @Query("""
        SELECT la FROM LeaveApplication la
        WHERE la.id = :applicationId
            AND la.employee.employeeId = :employeeId
    """)
    Optional<LeaveApplication> findByIdAndEmployeeId(Long applicationId, String employeeId);

    // 1️⃣ All leave applications for an employee
    @Query("""
        SELECT la FROM LeaveApplication la
        WHERE la.employee.employeeId = :employeeId
        ORDER BY la.appliedOn DESC
    """)
    List<LeaveApplication> findAllLeaves(@Param("employeeId") String employeeId);
}
