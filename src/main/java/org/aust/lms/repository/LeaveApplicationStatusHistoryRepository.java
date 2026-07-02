package org.aust.lms.repository;

import org.aust.lms.dto.StatusHistoryDto;
import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveApplicationStatusHistoryRepository extends JpaRepository<LeaveApplicationStatusHistory, Long> {

    @Query("""
        SELECT new org.aust.lms.dto.StatusHistoryDto(
            CAST(sh.actionTakenBy as string),
            CAST(sh.actionStatus as string),
            sh.actionTakenOn,
            sh.comment
        )
        FROM LeaveApplicationStatusHistory sh
        WHERE sh.applicationHistory.id = :historyId
        ORDER BY sh.actionTakenOn ASC
    """)
    List<StatusHistoryDto> findStatusHistoryByHistoryId(Long historyId);

    // 3️⃣ Latest non-SYSTEM status for a list of histories
//    @Query("""
//        SELECT lash FROM LeaveApplicationStatusHistory lash
//        WHERE lash.applicationHistory.id IN :historyIds
//          AND lash.actionTakenBy <> 'SYSTEM'
//          AND lash.actionTakenOn = (
//              SELECT MAX(lash2.actionTakenOn)
//              FROM LeaveApplicationStatusHistory lash2
//              WHERE lash2.applicationHistory.id = lash.applicationHistory.id
//                AND lash2.actionTakenBy <> 'SYSTEM'
//          )
//    """)
//    List<LeaveApplicationStatusHistory> findLatestNonSystemStatuses(@Param("historyIds") List<Long> historyIds);

    @Query("""
        select s
           from LeaveApplicationStatusHistory s
           where s.id in (
               select max(x.id)
               from LeaveApplicationStatusHistory x
               where x.applicationHistory.id in :historyIds
                 and x.actionTakenBy <> org.aust.lms.enums.LeaveActionRole.SYSTEM
               group by x.applicationHistory.id
           )
    """)
    List<LeaveApplicationStatusHistory> findLatestNonSystemStatuses(@Param("historyIds") List<Long> historyIds);

    @Query("""
        SELECT lash FROM LeaveApplicationStatusHistory lash
        WHERE lash.applicationHistory.id = :historyId
            ORDER BY lash.id DESC LIMIT 1
    """)
    Optional<LeaveApplicationStatusHistory> findLatestStatus(Long historyId);

    @Query("""
        SELECT lash FROM LeaveApplicationStatusHistory lash
        WHERE lash.applicationHistory.id IN :historyIds
            AND lash.actionTakenBy <> 'SYSTEM'
    """)
    List<LeaveApplicationStatusHistory> findNonSystemStatuses(List<Long> historyIds);

    @Query("""
        SELECT lash FROM LeaveApplicationStatusHistory lash
        WHERE lash.applicationHistory.id = :historyId
            AND lash.actionTakenBy <> 'SYSTEM'
    """)
    List<LeaveApplicationStatusHistory> findNonSystemStatuses(Long historyId);

    // Optional: filter APPROVED/REJECTED by role in JPQL
    @Query("""
        SELECT lash FROM LeaveApplicationStatusHistory lash
        WHERE lash.applicationHistory.application.employee.employeeId = :employeeId
          AND lash.actionTakenBy = :actionRole
          AND lash.actionStatus IN ('APPROVED','REJECTED')
          AND lash.actionTakenOn = (
              SELECT MAX(lash2.actionTakenOn)
              FROM LeaveApplicationStatusHistory lash2
              WHERE lash2.applicationHistory.id = lash.applicationHistory.id
                AND lash2.actionTakenBy <> 'SYSTEM'
          )
    """)
    List<LeaveApplicationStatusHistory> findApprovedOrRejectedByRole(@Param("employeeId") String employeeId,
                                                                     @Param("actionRole") String actionRole);
}
