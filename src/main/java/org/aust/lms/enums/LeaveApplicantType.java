package org.aust.lms.enums;

import lombok.*;

public enum LeaveApplicantType {
    COMMON(1, "Common"),
    TEACHER(2, "Teacher");

    final int value;
    final String description;

    LeaveApplicantType(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
