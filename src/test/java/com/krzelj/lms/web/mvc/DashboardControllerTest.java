package com.krzelj.lms.web.mvc;

import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.CourseService;
import com.krzelj.lms.service.UserService;
import com.krzelj.lms.web.api.ApiControllerTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private User testStudent;
    private User testInstructor;
    private User testAdmin;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testStudent = new User("student1", "hash", "student@test.com");
        testStudent.setId(1L);
        testStudent.setRoles(java.util.Set.of(new Role(RoleName.STUDENT)));

        testInstructor = new User("instructor1", "hash", "instructor@test.com");
        testInstructor.setId(2L);
        testInstructor.setRoles(java.util.Set.of(new Role(RoleName.INSTRUCTOR)));

        testAdmin = new User("admin1", "hash", "admin@test.com");
        testAdmin.setId(3L);
        testAdmin.setRoles(java.util.Set.of(new Role(RoleName.ADMIN)));

        testCourse = new Course("CS101", "Test Course", testInstructor);
        testCourse.setId(1L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void dashboard_AsStudent_RedirectsToStudentDashboard() throws Exception {
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/student"));

        verify(userService).findByUsername("student1");
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void dashboard_AsInstructor_RedirectsToInstructorDashboard() throws Exception {
        when(userService.findByUsername("instructor1")).thenReturn(Optional.of(testInstructor));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/instructor"));

        verify(userService).findByUsername("instructor1");
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void dashboard_AsAdmin_RedirectsToAdminDashboard() throws Exception {
        when(userService.findByUsername("admin1")).thenReturn(Optional.of(testAdmin));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));

        verify(userService).findByUsername("admin1");
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void studentDashboard_ReturnsViewWithCourses() throws Exception {
        List<Course> courses = Arrays.asList(testCourse);
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        when(courseService.findForStudent(1L)).thenReturn(courses);

        mockMvc.perform(get("/dashboard/student"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/student"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("courses"));

        verify(courseService).findForStudent(1L);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void instructorDashboard_ReturnsViewWithCourses() throws Exception {
        List<Course> courses = Arrays.asList(testCourse);
        when(userService.findByUsername("instructor1")).thenReturn(Optional.of(testInstructor));
        when(courseService.findForInstructor(2L)).thenReturn(courses);

        mockMvc.perform(get("/dashboard/instructor"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/instructor"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("courses"));

        verify(courseService).findForInstructor(2L);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void adminDashboard_ReturnsViewWithUsersAndCourses() throws Exception {
        List<User> users = Arrays.asList(testStudent, testInstructor, testAdmin);
        List<Course> courses = Arrays.asList(testCourse);
        when(userService.findAll()).thenReturn(users);
        when(courseService.findAll()).thenReturn(courses);

        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"))
                .andExpect(model().attribute("users", users))
                .andExpect(model().attribute("courses", courses))
                .andExpect(model().attribute("totalUsers", 3))
                .andExpect(model().attribute("totalCourses", 1));

        verify(userService).findAll();
        verify(courseService).findAll();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void adminDashboard_WithStudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isForbidden());

        verify(userService, never()).findAll();
    }
}
