package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.AssignmentRepository;
import com.krzelj.lms.repository.SubmissionRepository;
import com.krzelj.lms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public SubmissionService(SubmissionRepository submissionRepository,
                             AssignmentRepository assignmentRepository,
                             UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Submission getById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Submission> findForAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(assignmentId);
    }

    @Transactional(readOnly = true)
    public List<Submission> findForAssignmentWithDetails(Long assignmentId) {
        return submissionRepository.findByAssignmentIdWithStudentAndGraderOrderBySubmittedAtAsc(assignmentId);
    }

    @Transactional(readOnly = true)
    public List<Submission> findForStudent(Long studentId) {
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public Optional<Submission> findByAssignmentAndStudent(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
    }

    public Submission submitWork(Long assignmentId, Long studentId, String contentText) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(() -> new Submission(assignment, student));

        submission.setSubmittedAt(Instant.now());
        submission.setContentText(contentText);

        return submissionRepository.save(submission);
    }

    public Submission gradeSubmission(Long submissionId, Long graderUserId, Integer points) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        User grader = userRepository.findById(graderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Grader not found: " + graderUserId));

        submission.setGradePoints(points);
        submission.setGradedAt(Instant.now());
        submission.setGradedBy(grader);

        return submissionRepository.save(submission);
    }
}

