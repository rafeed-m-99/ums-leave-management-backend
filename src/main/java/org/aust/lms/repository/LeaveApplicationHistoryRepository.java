package org.aust.lms.repository;

import org.aust.lms.dto.LeaveApprovalListResponse;
import org.aust.lms.entity.LeaveApplicationHistory;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveApplicationHistoryRepository extends JpaRepository<LeaveApplicationHistory, Long> {

    public List<LeaveApplicationHistory> findLeaveApplicationHistoriesByNextApprovalRoleId(String nextApprovalRoleId);

    @Query("""
    SELECT new org.aust.lms.dto.LeaveApprovalListResponse(
        h.application.id,
        h.id,
        a.appliedOn,
        e.employeeId,
        e.shortName,
        d.designationName,
        CAST(h.applicationStage as string),
        lt.name,
        h.fromDate,
        h.toDate,
        h.totalDays,
        s.actionStatus
    )
    FROM LeaveApplicationHistory h
    JOIN h.application a
    JOIN a.employee e
    JOIN e.designation d
    JOIN a.leaveType lt
    JOIN LeaveApplicationStatusHistory s
        ON s.applicationHistory = h
    WHERE h.nextApprovalRoleId = :roleId
      AND s.actionTakenOn = (
          SELECT MAX(s2.actionTakenOn)
          FROM LeaveApplicationStatusHistory s2
          WHERE s2.applicationHistory = h
      )
      AND (:status IS NULL OR s.actionStatus = :status)
      AND (:applicationStage IS NULL OR h.applicationStage = :applicationStage)
      AND (:leaveType IS NULL OR lt.name = :leaveType)
""")
    Page<LeaveApprovalListResponse> findPendingByFilters(
            @Param("roleId") String roleId,
            @Param("status") LeaveActionStatus status,
            @Param("applicationStage") LeaveApplicationStage applicationStage,
            @Param("leaveType") String leaveType,
            Pageable pageable
    );

    @Query("""
        SELECT h
        FROM LeaveApplicationHistory h
        WHERE h.application.id = :applicationId
        AND h.createdOn = (
          SELECT MAX(h2.createdOn)
          FROM LeaveApplicationHistory h2
          WHERE h2.application.id = :applicationId
        )
    """)
    Optional<LeaveApplicationHistory> findLatestHistory(@Param("applicationId") Long applicationId);
}
