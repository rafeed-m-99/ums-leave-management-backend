package org.aust.lms.dto;

import java.time.Instant;

public record StatusHistoryDto (
    String actionBy,
    String status,
    Instant actionOn,
    String comment
) {}