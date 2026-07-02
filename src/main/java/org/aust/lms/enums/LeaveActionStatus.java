package org.aust.lms.enums;

import lombok.*;

@Getter
public enum LeaveActionStatus {
    WAITING (1, "Waiting"),
    REJECTED(2, "Rejected"),
    APPROVED(3, "Approved"),
    CANCELLED(4, "Cancelled");

    final int value;
    final String description;

    LeaveActionStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
