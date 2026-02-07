package com.krzelj.lms.web.api;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.service.AssignmentService;
import com.krzelj.lms.web.api.dto.AssignmentResponse;
import com.krzelj.lms.web.api.dto.CreateAssignmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/assignments")
public class AssignmentApiController {

    private final AssignmentService assignmentService;

    public AssignmentApiController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> listAssignments(@PathVariable Long courseId) {
        List<Assignment> assignments = assignmentService.findForCourse(courseId);
        List<AssignmentResponse> response = assignments.stream()
                .map(AssignmentResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignment(@PathVariable Long courseId,
                                                             @PathVariable Long assignmentId) {
        Assignment assignment = assignmentService.getById(assignmentId);
        return ResponseEntity.ok(AssignmentResponse.from(assignment));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateAssignmentRequest request) {
        Assignment assignment = assignmentService.createAssignment(
                courseId,
                request.title(),
                request.description() != null ? request.description() : "",
                request.dueAt(),
                request.maxPoints()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AssignmentResponse.from(assignment));
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long courseId,
                                                  @PathVariable Long assignmentId) {
        assignmentService.delete(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
