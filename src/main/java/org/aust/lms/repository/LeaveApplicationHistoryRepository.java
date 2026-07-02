package org.aust.lms.repository;

import org.aust.lms.dto.LeaveApprovalListResponse;
import org.aust.lms.entity.LeaveApplicationHistory;
import org.aust.lms.enums.LeaveActionStatus;
import org.aust.lms.enums.LeaveApplicationStage;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
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
      AND h.isActive = true
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
        AND h.isActive = true
    """)
    Optional<LeaveApplicationHistory> findLatestHistory(@Param("applicationId") Long applicationId);

    @Query("""
        SELECT h
        FROM LeaveApplicationHistory h
        WHERE h.application.id = :applicationId
        ORDER BY h.id ASC
    """)
    List<LeaveApplicationHistory> findAllHistories(@Param("applicationId") Long applicationId);

    // 2️⃣ Latest history for given leave application IDs
    @Query("""
        SELECT lah FROM LeaveApplicationHistory lah
        WHERE lah.application.id IN :applicationIds
          AND lah.createdOn = (
              SELECT MAX(lah2.createdOn)
              FROM LeaveApplicationHistory lah2
              WHERE lah2.application.id = lah.application.id
          )
          AND lah.isActive = true
    """)
    List<LeaveApplicationHistory> findLatestHistories(@Param("applicationIds") List<Long> applicationIds);

    @Query("""
        SELECT lah FROM LeaveApplicationHistory lah
        WHERE lah.application.id = :applicationId and lah.isActive = true
        ORDER BY lah.createdOn ASC
    """)
    List<LeaveApplicationHistory> findByApplicationIdOrderByCreatedOn(Long applicationId);

    @Query("""
        SELECT h
        FROM LeaveApplicationHistory h
        WHERE h.application.id = :applicationId
            AND h.applicationStage = org.aust.lms.enums.LeaveApplicationStage.INITIAL
    """)
    Optional<LeaveApplicationHistory> findInitialHistory(Long applicationId);

    @Query("""
        SELECT h
        FROM LeaveApplicationHistory h
        WHERE h.application.id = :applicationId
            AND h.id < :currentHistoryId
        ORDER BY h.id DESC
        LIMIT 1
    """)
    Optional<LeaveApplicationHistory> findPreviousHistory(
            Long applicationId,
            Long currentHistoryId);

    @Query("""
        SELECT COUNT(DISTINCT h.application.id)
        FROM LeaveApplicationHistory h
        WHERE h.application.employee.employeeId = :employeeId
        AND h.application.leaveType.id = :leaveTypeId
        AND h.isActive = true
        AND EXISTS (
            SELECT s
            FROM LeaveApplicationStatusHistory s
            WHERE s.applicationHistory = h
            AND s.actionStatus='APPROVED'
        )
    """)
    long countApprovedApplications(
            String employeeId,
            Long leaveTypeId
    );

    @Query("""
        SELECT COUNT(h)
        FROM LeaveApplicationHistory h
        JOIN h.application a
        JOIN LeaveApplicationStatusHistory s
        ON s.applicationHistory = h
        WHERE a.employee.employeeId = :employeeId
        AND h.isSandwichLeave = true
        AND h.isActive = true
        AND s.actionStatus = org.aust.lms.enums.LeaveActionStatus.APPROVED
        AND YEAR(h.fromDate) = :year
    """)
    long countApprovedSandwichLeaves(
            String employeeId,
            Integer year
    );
}
