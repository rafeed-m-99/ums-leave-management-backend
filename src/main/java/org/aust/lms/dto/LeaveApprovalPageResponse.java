package org.aust.lms.dto;

import org.springframework.data.domain.Page;

public record LeaveApprovalPageResponse (
        Page<LeaveApprovalListResponse> page,
        boolean isVC
) { }