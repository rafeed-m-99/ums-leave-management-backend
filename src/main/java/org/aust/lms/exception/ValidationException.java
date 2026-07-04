package org.aust.lms.exception;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<String> messages;

    public ValidationException(List<String> messages) {
        super("Validation failed");
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}