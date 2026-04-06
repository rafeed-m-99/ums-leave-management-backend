package org.aust.lms.dto;

public record LeaveBalanceResponse(
        Long id,
        Long leaveTypeId,
        String leaveTypeName,
        String genderApplicable,
        String applicantType,
        Integer daysLeft,
        Boolean sandwichLeaveTaken,
        Integer elHalfPayToFullPayConverted,
        Integer additionalElDays
) {}