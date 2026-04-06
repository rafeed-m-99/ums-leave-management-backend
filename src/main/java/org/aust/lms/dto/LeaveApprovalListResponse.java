package org.aust.lms.dto;

import org.aust.lms.enums.LeaveActionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveApprovalListResponse(
        Long applicationId,
        Long applicationHistoryId,
        Instant appliedOn,
        String employeeId,
        String employeeName,
        String designationName,
        String applicationStage,
        String leaveType,
        LocalDate fromDate,
        LocalDate toDate,
        Integer duration,
        LeaveActionStatus status
) {}