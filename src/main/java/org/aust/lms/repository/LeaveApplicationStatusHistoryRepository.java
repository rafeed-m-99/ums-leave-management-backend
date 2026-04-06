package org.aust.lms.repository;

import org.aust.lms.entity.LeaveApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApplicationStatusHistoryRepository extends JpaRepository<LeaveApplicationStatusHistory, Long> {
}
