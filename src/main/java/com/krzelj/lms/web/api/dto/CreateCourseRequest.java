package com.krzelj.lms.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 32, message = "Code must be at most 32 characters")
        String code,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        @NotNull(message = "Instructor ID is required")
        Long instructorId
) {
}
