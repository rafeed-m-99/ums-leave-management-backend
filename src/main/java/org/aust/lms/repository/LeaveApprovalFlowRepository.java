package org.aust.lms.repository;

import org.aust.lms.entity.LeaveApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveApprovalFlowRepository
        extends JpaRepository<LeaveApprovalFlow, Long> {

    Optional<LeaveApprovalFlow> findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
            Long leaveTypeId,
            Long designationId,
            Integer stepNumber
    );
}