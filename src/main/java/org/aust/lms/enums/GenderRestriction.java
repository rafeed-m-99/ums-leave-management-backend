package org.aust.lms.enums;

public enum GenderRestriction {
    ALL(1, "ALL"),
    MALE(2, "MALE"),
    FEMALE(3, "FEMALE");

    final int value;
    final String description;

    GenderRestriction(int value, String description) {
        this.value = value;
        this.description = description;
    }
}
