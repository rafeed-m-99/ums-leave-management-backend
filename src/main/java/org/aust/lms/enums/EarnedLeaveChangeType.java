package org.aust.lms.enums;

import lombok.*;

public enum EarnedLeaveChangeType {
    DATA_MIGRATION(1, "Data Migration"),
    SYSTEM_GENERATED(2, "System Generated"),
    APPLICATION(3, "Leave Application"),
    MODIFICATION(4, "Leave Modification"),
    CANCELLATION(5, "Leave Cancellation"),;

    final int value;
    final String description;

    EarnedLeaveChangeType(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
