package org.aust.lms.repository;

import org.aust.lms.entity.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
