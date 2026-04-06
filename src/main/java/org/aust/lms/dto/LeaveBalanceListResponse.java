package org.aust.lms.dto;

import java.util.List;

public record LeaveBalanceListResponse(
        String employeeId,
        Boolean isTeacher,
        Boolean isFemale,
        List<LeaveBalanceResponse> balances
) {}