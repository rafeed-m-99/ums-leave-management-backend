package org.aust.lms.enums;

public enum LeaveActionRole {
    APPLICANT(1, "Applicant"),
    HEAD(2, "Head/Director"),
    VC(3, "VC"),
    REGISTRAR(4, "Registrar"),
    SYSTEM(5, "System"),;

    final int value;
    final String description;

    LeaveActionRole(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
