package org.aust.lms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplicantLeaveDetailsForUpdateResponse(
        Long applicationId,
        Instant appliedOn,
        Long leaveTypeId,
        String leaveType,
        LocalDate from,
        LocalDate to,
        Integer duration,
        String reason,
        Boolean exBangladeshLeave,
        List<AttachmentDto> attachments, // empty for now
        String applicationStage
) {
}
