package com.krzelj.lms.web.mvc;

import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.AssignmentService;
import com.krzelj.lms.service.CourseService;
import com.krzelj.lms.service.SubmissionService;
import com.krzelj.lms.service.UserService;
import com.krzelj.lms.web.api.ApiControllerTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssignmentController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private Course testCourse;
    private Assignment testAssignment;
    private User testStudent;

    @BeforeEach
    void setUp() {
        User instructor = new User("instructor1", "hash", "inst@test.com");
        instructor.setId(1L);

        testCourse = new Course("CS101", "Test Course", instructor);
        testCourse.setId(1L);

        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plus(7, ChronoUnit.DAYS));
        testAssignment.setId(1L);

        testStudent = new User("student1", "hash", "stud@test.com");
        testStudent.setId(2L);
        testStudent.setRoles(Set.of(new Role(RoleName.STUDENT)));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listAssignments_ReturnsListView() throws Exception {
        when(courseService.getById(1L)).thenReturn(testCourse);
        when(assignmentService.findForCourse(1L)).thenReturn(List.of(testAssignment));

        mockMvc.perform(get("/courses/1/assignments"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignments/list"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("assignments"));

        verify(assignmentService).findForCourse(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void newAssignmentForm_AsInstructor_ReturnsNewView() throws Exception {
        when(courseService.getById(1L)).thenReturn(testCourse);

        mockMvc.perform(get("/courses/1/assignments/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignments/new"))
                .andExpect(model().attributeExists("course"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void newAssignmentForm_AsStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/courses/1/assignments/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void createAssignment_AsInstructor_RedirectsToList() throws Exception {
        when(assignmentService.createAssignment(eq(1L), eq("HW2"), eq("Desc"), any(Instant.class), eq(100)))
                .thenReturn(testAssignment);

        mockMvc.perform(post("/courses/1/assignments")
                        .param("title", "HW2")
                        .param("description", "Desc")
                        .param("dueAt", "2026-06-01T23:59:00")
                        .param("maxPoints", "100")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/assignments"));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void viewAssignment_AsStudent_ReturnsDetailView() throws Exception {
        when(courseService.getById(1L)).thenReturn(testCourse);
        when(assignmentService.getByIdWithCourse(1L)).thenReturn(testAssignment);
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        when(submissionService.findByAssignmentAndStudent(1L, 2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/courses/1/assignments/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignments/detail"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("assignment"));
    }
}
