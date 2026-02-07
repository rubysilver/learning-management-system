package com.krzelj.lms.web.api;

import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.service.SubmissionService;
import com.krzelj.lms.service.UserService;
import com.krzelj.lms.web.api.dto.GradeSubmissionRequest;
import com.krzelj.lms.web.api.dto.SubmissionResponse;
import com.krzelj.lms.web.api.dto.SubmitWorkRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments/{assignmentId}/submissions")
public class SubmissionApiController {

    private final SubmissionService submissionService;
    private final UserService userService;

    public SubmissionApiController(SubmissionService submissionService, UserService userService) {
        this.submissionService = submissionService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<SubmissionResponse>> listSubmissions(@PathVariable Long assignmentId) {
        List<Submission> submissions = submissionService.findForAssignment(assignmentId);
        List<SubmissionResponse> response = submissions.stream()
                .map(SubmissionResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionResponse> getSubmission(@PathVariable Long assignmentId,
                                                             @PathVariable Long submissionId) {
        Submission submission = submissionService.getById(submissionId);
        return ResponseEntity.ok(SubmissionResponse.from(submission));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionResponse> submitWork(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmitWorkRequest request,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Submission submission = submissionService.submitWork(
                assignmentId,
                currentUser.getId(),
                request.contentText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SubmissionResponse.from(submission));
    }

    @PutMapping("/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestParam Long graderUserId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        Submission submission = submissionService.gradeSubmission(
                submissionId,
                graderUserId,
                request.points()
        );
        return ResponseEntity.ok(SubmissionResponse.from(submission));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsForStudent(@PathVariable Long assignmentId,
                                                                              @PathVariable Long studentId) {
        List<Submission> submissions = submissionService.findForStudent(studentId);
        List<SubmissionResponse> response = submissions.stream()
                .map(SubmissionResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
