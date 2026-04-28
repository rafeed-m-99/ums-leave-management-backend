package org.aust.lms.dto;

public record AttachmentDto (
        Long id,
        String fileName,
        String fileType,
        String description
) { }