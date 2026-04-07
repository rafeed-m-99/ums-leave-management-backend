package org.aust.lms.dto;

import org.aust.lms.enums.LeaveActionStatus;

public record LeaveApprovalRequest(
        Long applicationId,
        String action,
        String comment,
        String roleId
) {}