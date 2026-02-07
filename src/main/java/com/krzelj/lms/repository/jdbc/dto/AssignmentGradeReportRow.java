package com.krzelj.lms.repository.jdbc.dto;

public record AssignmentGradeReportRow(
        long assignmentId,
        long courseId,
        long submissionsCount,
        long gradedCount,
        Double averagePoints
) {
}

