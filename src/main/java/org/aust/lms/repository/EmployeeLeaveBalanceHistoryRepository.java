package org.aust.lms.repository;

import org.aust.lms.entity.EmployeeLeaveBalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLeaveBalanceHistoryRepository extends JpaRepository<EmployeeLeaveBalanceHistory, Long> {
}
