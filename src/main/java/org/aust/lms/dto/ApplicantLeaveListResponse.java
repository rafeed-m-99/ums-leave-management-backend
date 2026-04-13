package org.aust.lms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ApplicantLeaveListResponse(
        Long applicationId,
        Instant appliedOn,
        String leaveType,
        LocalDate from,
        LocalDate to,
        Integer duration,
        String applicationStage,
        String status,
        String actionTakenBy,
        String nextRole
) {}