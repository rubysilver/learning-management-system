package com.krzelj.lms.web.api.dto;

import com.krzelj.lms.domain.Course;

import java.time.Instant;

public record CourseResponse(
        Long id,
        String code,
        String title,
        String description,
        Long instructorId,
        String instructorName,
        Instant createdAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                course.getInstructor().getId(),
                course.getInstructor().getUsername(),
                course.getCreatedAt()
        );
    }
}
