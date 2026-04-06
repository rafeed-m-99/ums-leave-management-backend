package org.aust.lms.enums;

import lombok.*;

public enum LeaveApplicationStage {
    INITIAL(1, "Initial"),
    MODIFICATION(2, "Applied for Modification"),
    CANCELLATION(3, "Applied for Cancellation"),;

    final int value;
    final String description;

    LeaveApplicationStage(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
