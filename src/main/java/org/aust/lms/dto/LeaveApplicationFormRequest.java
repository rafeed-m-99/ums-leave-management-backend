package org.aust.lms.dto;

import java.util.List;

public record LeaveApplicationFormRequest(
        Long leaveTypeId,
        String from,
        String to,
        Boolean exBdLeave,
        Integer applicationStep,
        String reason
) {

    public record Attachment(
            String fileName,
            String description
    ) {}
}