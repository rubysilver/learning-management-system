package com.krzelj.lms.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitWorkRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 10000, message = "Content must be at most 10000 characters")
        String contentText
) {
}
