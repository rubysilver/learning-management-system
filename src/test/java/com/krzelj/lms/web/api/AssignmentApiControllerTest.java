package com.krzelj.lms.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.AssignmentService;
import com.krzelj.lms.web.api.dto.CreateAssignmentRequest;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignmentApiController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class AssignmentApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper;

    private Assignment testAssignment;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule for Instant serialization
        
        testCourse = new Course("CS101", "Test Course", null);
        testCourse.setId(1L);
        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plusSeconds(86400));
        testAssignment.setId(1L);
        testAssignment.setDescription("Test assignment");
        testAssignment.setMaxPoints(100);
    }

    @Test
    @WithMockUser
    void listAssignments_ReturnsListOfAssignments() throws Exception {
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentService.findForCourse(1L)).thenReturn(assignments);

        mockMvc.perform(get("/api/courses/1/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(assignmentService).findForCourse(1L);
    }

    @Test
    @WithMockUser
    void getAssignment_WhenExists_ReturnsAssignment() throws Exception {
        when(assignmentService.getById(1L)).thenReturn(testAssignment);

        mockMvc.perform(get("/api/courses/1/assignments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Homework 1"));

        verify(assignmentService).getById(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void createAssignment_WithInstructorRole_Success() throws Exception {
        CreateAssignmentRequest request = new CreateAssignmentRequest(
                "New Assignment", "Description", Instant.now().plusSeconds(86400), 100);
        when(assignmentService.createAssignment(anyLong(), anyString(), anyString(), any(Instant.class), anyInt()))
                .thenReturn(testAssignment);

        mockMvc.perform(post("/api/courses/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(assignmentService).createAssignment(anyLong(), anyString(), anyString(), any(Instant.class), anyInt());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createAssignment_WithStudentRole_ReturnsForbidden() throws Exception {
        CreateAssignmentRequest request = new CreateAssignmentRequest(
                "New Assignment", "Description", Instant.now().plusSeconds(86400), 100);

        mockMvc.perform(post("/api/courses/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(assignmentService, never()).createAssignment(anyLong(), anyString(), anyString(), any(Instant.class), anyInt());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void deleteAssignment_WithInstructorRole_Success() throws Exception {
        doNothing().when(assignmentService).delete(1L);

        mockMvc.perform(delete("/api/courses/1/assignments/1"))
                .andExpect(status().isNoContent());

        verify(assignmentService).delete(1L);
    }
}
