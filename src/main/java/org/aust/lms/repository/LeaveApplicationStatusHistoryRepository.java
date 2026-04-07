package org.aust.lms.repository;

import org.aust.lms.dto.StatusHistoryDto;
import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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
}
