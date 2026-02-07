package com.krzelj.lms.web.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAssignmentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        @NotNull(message = "Due date is required")
        Instant dueAt,

        @Min(value = 1, message = "Max points must be at least 1")
        int maxPoints
) {
}
