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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private User testInstructor;
    private User testStudent;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testInstructor = new User("instructor1", "hash", "inst@test.com");
        testInstructor.setId(1L);
        testInstructor.setRoles(Set.of(new Role(RoleName.INSTRUCTOR)));

        testStudent = new User("student1", "hash", "stud@test.com");
        testStudent.setId(2L);
        testStudent.setRoles(Set.of(new Role(RoleName.STUDENT)));

        testCourse = new Course("CS101", "Test Course", testInstructor);
        testCourse.setId(1L);
        testCourse.setStudents(new HashSet<>());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listCourses_ReturnsListView() throws Exception {
        when(courseService.findAll()).thenReturn(List.of(testCourse));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/list"))
                .andExpect(model().attributeExists("courses"));

        verify(courseService).findAll();
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void viewCourse_ReturnsDetailView() throws Exception {
        when(courseService.getById(1L)).thenReturn(testCourse);
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attributeExists("course"));

        verify(courseService).getById(1L);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void newCourseForm_AsInstructor_ReturnsNewView() throws Exception {
        when(userService.findInstructors()).thenReturn(List.of(testInstructor));

        mockMvc.perform(get("/courses/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/new"))
                .andExpect(model().attributeExists("instructors"));

        verify(userService).findInstructors();
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void newCourseForm_AsStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/courses/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void createCourse_AsAdmin_RedirectsToAdminDashboard() throws Exception {
        when(courseService.createCourse("CS102", "New Course", "Desc", 1L)).thenReturn(testCourse);

        mockMvc.perform(post("/courses")
                        .param("code", "CS102")
                        .param("title", "New Course")
                        .param("description", "Desc")
                        .param("instructorId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));

        verify(courseService).createCourse("CS102", "New Course", "Desc", 1L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void enrollInCourse_AsStudent_RedirectsToCourse() throws Exception {
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        doNothing().when(courseService).enrollStudent(1L, 2L);

        mockMvc.perform(post("/courses/1/enroll")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"));

        verify(courseService).enrollStudent(1L, 2L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void unenrollFromCourse_AsStudent_RedirectsToCourse() throws Exception {
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        doNothing().when(courseService).unenrollStudent(1L, 2L);

        mockMvc.perform(post("/courses/1/unenroll")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"));

        verify(courseService).unenrollStudent(1L, 2L);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void deleteCourse_AsAdmin_RedirectsToAdminDashboard() throws Exception {
        doNothing().when(courseService).delete(1L);

        mockMvc.perform(post("/courses/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));

        verify(courseService).delete(1L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void deleteCourse_AsStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/courses/1/delete")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(courseService, never()).delete(any());
    }
}
