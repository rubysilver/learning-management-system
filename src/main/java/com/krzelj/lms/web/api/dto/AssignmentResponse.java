package com.krzelj.lms.web.api.dto;

import com.krzelj.lms.domain.Assignment;

import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long courseId,
        String courseTitle,
        String title,
        String description,
        Instant dueAt,
        int maxPoints
) {
    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getTitle(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt(),
                assignment.getMaxPoints()
        );
    }
}
