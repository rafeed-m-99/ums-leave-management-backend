package org.aust.lms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplicantLeaveDetailsResponse(
        Long applicationId,
        Instant appliedOn,
        String leaveType,
        LocalDate from,
        LocalDate to,
        Integer duration,
        String reason,
        List<AttachmentDto> attachments, // empty for now
        String applicationStage,
        String status,
        String actionTakenBy,
        Instant actionTakenOn
) {}