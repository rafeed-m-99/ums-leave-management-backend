package org.aust.lms.dto;

public record AttachmentDto (
        String fileName,
        String description,
        String fileUrl
) { }