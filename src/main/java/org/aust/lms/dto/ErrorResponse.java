package org.aust.lms.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        Instant timestamp,
        List<String> messages
) { }