package org.aust.lms.enums;

import lombok.*;

public enum LeaveSalaryType {
    WITH_PAY(1, "With pay"),
    HALF_PAY(2, "Half pay"),
    WITHOUT_PAY(3, "Withour pay");

    final int value;
    final String description;

    LeaveSalaryType(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
