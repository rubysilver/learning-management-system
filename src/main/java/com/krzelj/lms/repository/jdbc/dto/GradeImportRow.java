package com.krzelj.lms.repository.jdbc.dto;

public record GradeImportRow(
        long assignmentId,
        long studentId,
        Integer gradePoints
) {
}

