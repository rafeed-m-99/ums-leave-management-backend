package org.aust.lms.dto;

import java.util.List;

public record LeaveApplicationResponse(
        boolean success,
        List<String> messages
) { }
