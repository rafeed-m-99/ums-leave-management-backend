package org.aust.lms.dto;

public record LeaveAttachmentRequest(
        String storedFileName,
        String originalFileName,
        String fileType,
        Long fileSize,
        String description
) { }
