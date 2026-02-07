package com.krzelj.lms.web.mvc;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.service.AssignmentService;
import com.krzelj.lms.service.CourseService;
import com.krzelj.lms.service.SubmissionService;
import com.krzelj.lms.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Controller
@RequestMapping("/courses/{courseId}/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final CourseService courseService;
    private final SubmissionService submissionService;
    private final UserService userService;

    public AssignmentController(AssignmentService assignmentService, CourseService courseService,
                               SubmissionService submissionService, UserService userService) {
        this.assignmentService = assignmentService;
        this.courseService = courseService;
        this.submissionService = submissionService;
        this.userService = userService;
    }

    @GetMapping
    public String listAssignments(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", courseService.getById(courseId));
        model.addAttribute("assignments", assignmentService.findForCourse(courseId));
        return "assignments/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public String newAssignmentForm(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", courseService.getById(courseId));
        return "assignments/new";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public String createAssignment(@PathVariable Long courseId,
                                   @RequestParam String title,
                                   @RequestParam(required = false, defaultValue = "") String description,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
                                   @RequestParam(defaultValue = "100") int maxPoints) {
        Instant dueInstant = dueAt.toInstant(ZoneOffset.UTC);
        Assignment assignment = assignmentService.createAssignment(courseId, title, description, dueInstant, maxPoints);
        return "redirect:/courses/" + courseId + "/assignments";
    }

    @GetMapping("/{assignmentId}")
    public String viewAssignment(@PathVariable Long courseId, @PathVariable Long assignmentId,
                                @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("course", courseService.getById(courseId));
        Assignment assignment = assignmentService.getByIdWithCourse(assignmentId);
        model.addAttribute("assignment", assignment);

        if (userDetails != null) {
            Optional<User> currentUser = userService.findByUsername(userDetails.getUsername());
            currentUser.ifPresent(user -> {
                model.addAttribute("currentUser", user);
                model.addAttribute("mySubmission",
                        submissionService.findByAssignmentAndStudent(assignmentId, user.getId()).orElse(null));
            });
        }
        return "assignments/detail";
    }
}

