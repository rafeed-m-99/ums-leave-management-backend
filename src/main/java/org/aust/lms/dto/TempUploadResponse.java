package org.aust.lms.dto;

public record TempUploadResponse(
        String storedFileName,
        String originalFileName,
        String fileType,
        long size
) {}