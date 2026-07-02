package org.aust.lms.dto;

import java.time.LocalDateTime;

public record LeaveApplicationTimelineResponse(
        String status,
        String actionBy,
        String comment,
        LocalDateTime time
) {
}
