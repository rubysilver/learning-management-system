package com.krzelj.lms.web.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeSubmissionRequest(
        @NotNull(message = "Points are required")
        @Min(value = 0, message = "Points must be at least 0")
        Integer points
) {
}
