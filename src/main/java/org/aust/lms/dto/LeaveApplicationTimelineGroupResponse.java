package org.aust.lms.dto;

import java.util.List;

public record LeaveApplicationTimelineGroupResponse(
        String stage,
        List<LeaveApplicationTimelineResponse> items
) {
}