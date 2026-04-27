package org.aust.lms.dto;

import java.util.List;

public record LeaveApplicationResponse(
        Long applicationId,
        boolean success,
        List<String> messages
) { }
