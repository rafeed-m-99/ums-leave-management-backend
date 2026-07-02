package org.aust.lms.dto;

import java.util.List;

public record LeaveApplicationFormRequestSub(
        Long leaveTypeId,
        String from,
        String to,
        Boolean exBdLeave,
        Integer applicationStep,
        String reason,
        List<LeaveAttachmentRequest> attachments,
        String substituteId
) { }
