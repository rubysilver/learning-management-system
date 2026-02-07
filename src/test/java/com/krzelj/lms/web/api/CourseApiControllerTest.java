package com.krzelj.lms.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.CourseService;
import com.krzelj.lms.web.api.dto.CreateCourseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseApiController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class CourseApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper;

    private Course testCourse;
    private User testInstructor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule for Instant serialization
        
        testInstructor = new User("instructor1", "hash", "instructor@test.com");
        testInstructor.setId(1L);
        testInstructor.setRoles(Set.of(new Role(RoleName.INSTRUCTOR)));

        testCourse = new Course("CS101", "Introduction to Computer Science", testInstructor);
        testCourse.setId(1L);
        testCourse.setDescription("Test course");
    }

    @Test
    @WithMockUser
    void listCourses_ReturnsListOfCourses() throws Exception {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseService.findAll()).thenReturn(courses);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].code").value("CS101"));

        verify(courseService).findAll();
    }

    @Test
    @WithMockUser
    void getCourse_WhenExists_ReturnsCourse() throws Exception {
        when(courseService.getById(1L)).thenReturn(testCourse);

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.code").value("CS101"));

        verify(courseService).getById(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void getCoursesForInstructor_WithInstructorRole_ReturnsCourses() throws Exception {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseService.findForInstructor(1L)).thenReturn(courses);

        mockMvc.perform(get("/api/courses/instructor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(courseService).findForInstructor(1L);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getCoursesForInstructor_WithStudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/courses/instructor/1"))
                .andExpect(status().isForbidden());

        verify(courseService, never()).findForInstructor(any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getCoursesForStudent_WithStudentRole_ReturnsCourses() throws Exception {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseService.findForStudent(1L)).thenReturn(courses);

        mockMvc.perform(get("/api/courses/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(courseService).findForStudent(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void createCourse_WithInstructorRole_Success() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("CS102", "New Course", "Description", 1L);
        when(courseService.createCourse(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(testCourse);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(courseService).createCourse(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createCourse_WithStudentRole_ReturnsForbidden() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("CS102", "New Course", "Description", 1L);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(courseService, never()).createCourse(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCourse_WithAdminRole_Success() throws Exception {
        doNothing().when(courseService).delete(1L);

        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isNoContent());

        verify(courseService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void deleteCourse_WithInstructorRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isForbidden());

        verify(courseService, never()).delete(any());
    }
}
