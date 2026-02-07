package com.krzelj.lms.web.mvc;

import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.service.CourseService;
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

import java.util.List;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;

    public CourseController(CourseService courseService, UserService userService) {
        this.courseService = courseService;
        this.userService = userService;
    }

    @GetMapping
    public String listCourses(Model model) {
        List<Course> courses = courseService.findAll();
        model.addAttribute("courses", courses);
        return "courses/list";
    }

    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id, 
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        try {
            Course course = courseService.getById(id);

            User instructor = course.getInstructor();
            String instructorUsername = "Unknown";
            Long instructorId = null;
            if (instructor != null) {
                instructorUsername = instructor.getUsername();
                instructorId = instructor.getId();
            }

            java.util.Set<User> students = course.getStudents();
            int studentCount = students != null ? students.size() : 0;

            model.addAttribute("course", course);
            model.addAttribute("instructor", instructor);
            model.addAttribute("instructorUsername", instructorUsername);
            model.addAttribute("instructorId", instructorId);
            model.addAttribute("studentCount", studentCount);

            model.addAttribute("isEnrolled", false);
            model.addAttribute("isInstructor", false);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isStudent", false);
            model.addAttribute("currentUser", null);

            if (userDetails != null) {
                User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
                if (currentUser != null) {
                    model.addAttribute("currentUser", currentUser);
                    Long currentUserId = currentUser.getId();

                    boolean isEnrolled = false;
                    if (students != null) {
                        isEnrolled = students.stream()
                                .anyMatch(s -> s.getId().equals(currentUserId));
                    }
                    model.addAttribute("isEnrolled", isEnrolled);

                    boolean isInstructor = instructorId != null && instructorId.equals(currentUserId);
                    model.addAttribute("isInstructor", isInstructor);

                    boolean isAdmin = userDetails.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                    model.addAttribute("isAdmin", isAdmin);

                    boolean isStudent = userDetails.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));
                    model.addAttribute("isStudent", isStudent);
                }
            }
            
            return "courses/detail";
        } catch (IllegalArgumentException e) {
            return "redirect:/courses?error=" + java.net.URLEncoder.encode("Course not found", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/courses?error=" + java.net.URLEncoder.encode("Failed to load course: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public String newCourseForm(Model model) {
        List<User> instructors = userService.findInstructors();
        model.addAttribute("instructors", instructors);
        return "courses/new";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public String createCourse(@RequestParam String code,
                               @RequestParam String title,
                               @RequestParam(required = false, defaultValue = "") String description,
                               @RequestParam Long instructorId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.createCourse(code, title, description, instructorId);
            redirectAttributes.addFlashAttribute("success", "Course created successfully");

            if (userDetails != null) {
                boolean isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                if (isAdmin) {
                    return "redirect:/dashboard/admin";
                } else {
                    return "redirect:/dashboard/instructor";
                }
            }
            return "redirect:/courses";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create course: " + e.getMessage());
            return "redirect:/courses/new";
        }
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public String enrollInCourse(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            courseService.enrollStudent(id, student.getId());
            redirectAttributes.addFlashAttribute("success", "Successfully enrolled in course");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to enroll: " + e.getMessage());
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/unenroll")
    @PreAuthorize("hasRole('STUDENT')")
    public String unenrollFromCourse(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            courseService.unenrollStudent(id, student.getId());
            redirectAttributes.addFlashAttribute("success", "Successfully unenrolled from course");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to unenroll: " + e.getMessage());
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Course deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete course: " + e.getMessage());
        }
        return "redirect:/dashboard/admin";
    }
}

