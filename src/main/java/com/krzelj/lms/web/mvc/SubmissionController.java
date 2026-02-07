package com.krzelj.lms.web.mvc;

import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.jdbc.GradeImportJdbcRepository;
import com.krzelj.lms.repository.jdbc.dto.GradeImportRow;
import com.krzelj.lms.service.AssignmentService;
import com.krzelj.lms.service.SubmissionService;
import com.krzelj.lms.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/assignments/{assignmentId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final AssignmentService assignmentService;
    private final UserService userService;
    private final GradeImportJdbcRepository gradeImportJdbcRepository;

    public SubmissionController(SubmissionService submissionService, AssignmentService assignmentService,
                               UserService userService, GradeImportJdbcRepository gradeImportJdbcRepository) {
        this.submissionService = submissionService;
        this.assignmentService = assignmentService;
        this.userService = userService;
        this.gradeImportJdbcRepository = gradeImportJdbcRepository;
    }

    @GetMapping
    public String listSubmissions(@PathVariable Long assignmentId,
                                 @AuthenticationPrincipal UserDetails userDetails, Model model) {
        var assignment = assignmentService.getByIdWithCourse(assignmentId);
        model.addAttribute("assignment", assignment);

        boolean isStudent = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));
        boolean isInstructorOrAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_INSTRUCTOR") || auth.getAuthority().equals("ROLE_ADMIN"));

        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()).orElse(null) : null;
        List<Submission> submissions;
        if (isStudent && !isInstructorOrAdmin) {
            submissions = currentUser != null
                    ? submissionService.findForAssignmentWithDetails(assignmentId).stream()
                            .filter(s -> s.getStudent().getId().equals(currentUser.getId()))
                            .toList()
                    : List.of();
        } else {
            submissions = submissionService.findForAssignmentWithDetails(assignmentId);
        }
        model.addAttribute("submissions", submissions);
        model.addAttribute("isInstructorOrAdmin", isInstructorOrAdmin);
        model.addAttribute("currentUser", currentUser);
        return "submissions/list";
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public String submitWork(@PathVariable Long assignmentId,
                             @RequestParam String contentText,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        submissionService.submitWork(assignmentId, currentUser.getId(), contentText);
        var assignment = assignmentService.getByIdWithCourse(assignmentId);
        return "redirect:/courses/" + assignment.getCourse().getId() + "/assignments/" + assignmentId;
    }

    @PostMapping("/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public String gradeSubmission(@PathVariable Long assignmentId,
                                  @PathVariable Long submissionId,
                                  @RequestParam Long graderUserId,
                                  @RequestParam Integer points) {
        submissionService.gradeSubmission(submissionId, graderUserId, points);
        return "redirect:/assignments/" + assignmentId + "/submissions";
    }

    @PostMapping("/bulk-grade")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public String bulkGrade(@PathVariable Long assignmentId,
                            @RequestParam String bulkGrades,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        User grader = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        List<GradeImportRow> rows = new ArrayList<>();
        for (String line : bulkGrades.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("[, \t]+");
            if (parts.length >= 2) {
                try {
                    long studentId = Long.parseLong(parts[0].trim());
                    int points = Integer.parseInt(parts[1].trim());
                    rows.add(new GradeImportRow(assignmentId, studentId, points));
                } catch (NumberFormatException ignored) { }
            }
        }
        if (rows.isEmpty()) {
            redirectAttributes.addFlashAttribute("bulkGradeError", "No valid lines. Use: studentId,points (one per line).");
            return "redirect:/assignments/" + assignmentId + "/submissions";
        }
        int[][] result = gradeImportJdbcRepository.batchUpdateGrades(rows, grader.getId(), Instant.now());
        int updated = 0;
        for (int[] batch : result) {
            for (int c : batch) updated += c;
        }
        redirectAttributes.addFlashAttribute("bulkGradeSuccess", updated + " of " + rows.size() + " grades updated.");
        return "redirect:/assignments/" + assignmentId + "/submissions";
    }
}

