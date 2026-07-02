package org.aust.lms.repository;

import org.aust.lms.entity.LeavePolicy;
import org.aust.lms.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    Optional<LeavePolicy> findByLeaveType(LeaveType leaveType);
}
