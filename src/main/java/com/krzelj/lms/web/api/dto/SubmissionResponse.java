package com.krzelj.lms.web.api.dto;

import com.krzelj.lms.domain.Submission;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        Instant submittedAt,
        Integer gradePoints,
        Instant gradedAt,
        Long gradedById,
        String gradedByName,
        String contentText
) {
    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getUsername(),
                submission.getSubmittedAt(),
                submission.getGradePoints(),
                submission.getGradedAt(),
                submission.getGradedBy() != null ? submission.getGradedBy().getId() : null,
                submission.getGradedBy() != null ? submission.getGradedBy().getUsername() : null,
                submission.getContentText()
        );
    }
}
