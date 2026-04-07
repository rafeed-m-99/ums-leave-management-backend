package org.aust.lms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LeaveApplicationDetailsResponse(
        Long applicationId,
        String employeeId,
        String employeeName,
        String designation,
        String leaveType,
        Integer daysLeft,
        LocalDate fromDate,
        LocalDate toDate,
        Integer totalDays,
        Instant appliedOn,
        String reason,
        List<AttachmentDto> attachments,
        List<StatusHistoryDto> history
) {}