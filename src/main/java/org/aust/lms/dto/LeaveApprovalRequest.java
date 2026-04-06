package org.aust.lms.dto;

import org.aust.lms.enums.LeaveActionStatus;

public record LeaveApprovalRequest(
        Long applicationHistoryId,
        LeaveActionStatus action,
        String comment,
        String roleId
) {}