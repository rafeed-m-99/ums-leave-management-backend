package org.aust.lms.enums;

import lombok.*;

public enum LeaveApprovalRole {
    APPLICANT(1, "Applicant"),
    HEAD(2, "Head/Director"),
    VC(3, "VC"),
    REGISTRAR(4, "Registrar");

    final int value;
    final String description;

    LeaveApprovalRole(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
