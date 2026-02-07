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

import java.util.List;

@Controller
public class DashboardController {

    private final CourseService courseService;
    private final UserService userService;

    public DashboardController(CourseService courseService, UserService userService) {
        this.courseService = courseService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/";
        }

        String username = userDetails.getUsername();
        User user = userService.findByUsername(username).orElse(null);

        if (user == null) {
            return "redirect:/";
        }

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isInstructor = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_INSTRUCTOR"));
        boolean isStudent = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin) {
            return "redirect:/dashboard/admin";
        } else if (isInstructor) {
            return "redirect:/dashboard/instructor";
        } else if (isStudent) {
            return "redirect:/dashboard/student";
        }

        return "redirect:/";
    }

    @GetMapping("/dashboard/student")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/";
        }

        String username = userDetails.getUsername();
        User user = userService.findByUsername(username).orElse(null);

        if (user != null) {
            List<Course> enrolledCourses = courseService.findForStudent(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("courses", enrolledCourses);
        }

        return "dashboard/student";
    }

    @GetMapping("/dashboard/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public String instructorDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/";
        }

        String username = userDetails.getUsername();
        User user = userService.findByUsername(username).orElse(null);

        if (user != null) {
            List<Course> teachingCourses = courseService.findForInstructor(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("courses", teachingCourses);
        }

        return "dashboard/instructor";
    }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        List<User> allUsers = userService.findAll();
        List<Course> allCourses = courseService.findAll();

        model.addAttribute("users", allUsers);
        model.addAttribute("courses", allCourses);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalCourses", allCourses.size());

        return "dashboard/admin";
    }
}
