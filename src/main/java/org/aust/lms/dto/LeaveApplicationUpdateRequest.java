package org.aust.lms.dto;

public record LeaveApplicationUpdateRequest(
        String from,
        String to,
        Boolean exBdLeave,
        Integer applicationStep,
        String reason
) { }
