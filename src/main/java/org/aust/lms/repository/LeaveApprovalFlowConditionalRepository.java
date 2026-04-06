package org.aust.lms.repository;

import org.aust.lms.entity.LeaveApprovalFlowConditional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveApprovalFlowConditionalRepository
        extends JpaRepository<LeaveApprovalFlowConditional, Long> {

    Optional<LeaveApprovalFlowConditional> findByLeaveTypeIdAndEmployeeDesignationDesignationIdAndStepNumber(
            Long leaveTypeId,
            Long designationId,
            Integer stepNumber
    );
}