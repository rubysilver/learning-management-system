package com.krzelj.lms.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.SubmissionService;
import com.krzelj.lms.service.UserService;
import com.krzelj.lms.web.api.dto.GradeSubmissionRequest;
import com.krzelj.lms.web.api.dto.SubmitWorkRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionApiController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class SubmissionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper;

    private Assignment testAssignment;
    private User testStudent;
    private Submission testSubmission;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        Course testCourse = new Course("CS101", "Test Course", null);
        testCourse.setId(1L);
        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plusSeconds(86400));
        testAssignment.setId(1L);

        testStudent = new User("student1", "hash", "student@test.com");
        testStudent.setId(2L);

        testSubmission = new Submission(testAssignment, testStudent);
        testSubmission.setId(1L);
        testSubmission.setContentText("My submission");
        testSubmission.setSubmittedAt(Instant.now());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void listSubmissions_WithInstructorRole_ReturnsList() throws Exception {
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionService.findForAssignment(1L)).thenReturn(submissions);

        mockMvc.perform(get("/api/assignments/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].assignmentId").value(1))
                .andExpect(jsonPath("$[0].studentId").value(2));

        verify(submissionService).findForAssignment(1L);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listSubmissions_WithStudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/assignments/1/submissions"))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).findForAssignment(anyLong());
    }

    @Test
    @WithMockUser
    void getSubmission_WhenExists_ReturnsSubmission() throws Exception {
        when(submissionService.getById(1L)).thenReturn(testSubmission);

        mockMvc.perform(get("/api/assignments/1/submissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.assignmentTitle").value("Homework 1"))
                .andExpect(jsonPath("$.studentName").value("student1"));

        verify(submissionService).getById(1L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void submitWork_WithStudentRole_ReturnsCreated() throws Exception {
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        when(submissionService.submitWork(eq(1L), eq(2L), eq("My content")))
                .thenReturn(testSubmission);

        SubmitWorkRequest request = new SubmitWorkRequest("My content");

        mockMvc.perform(post("/api/assignments/1/submissions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(userService).findByUsername("student1");
        verify(submissionService).submitWork(eq(1L), eq(2L), eq("My content"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void submitWork_WithInstructorRole_ReturnsForbidden() throws Exception {
        SubmitWorkRequest request = new SubmitWorkRequest("My content");

        mockMvc.perform(post("/api/assignments/1/submissions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).submitWork(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void gradeSubmission_WithInstructorRole_ReturnsOk() throws Exception {
        testSubmission.setGradePoints(85);
        testSubmission.setGradedAt(Instant.now());
        when(submissionService.gradeSubmission(eq(1L), eq(2L), eq(85)))
                .thenReturn(testSubmission);

        GradeSubmissionRequest request = new GradeSubmissionRequest(85);

        mockMvc.perform(put("/api/assignments/1/submissions/1/grade")
                        .param("graderUserId", "2")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(submissionService).gradeSubmission(eq(1L), eq(2L), eq(85));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void gradeSubmission_WithStudentRole_ReturnsForbidden() throws Exception {
        GradeSubmissionRequest request = new GradeSubmissionRequest(85);

        mockMvc.perform(put("/api/assignments/1/submissions/1/grade")
                        .param("graderUserId", "2")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).gradeSubmission(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getSubmissionsForStudent_WithStudentRole_ReturnsList() throws Exception {
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionService.findForStudent(2L)).thenReturn(submissions);

        mockMvc.perform(get("/api/assignments/1/submissions/student/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(submissionService).findForStudent(2L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void getSubmissionsForStudent_WithInstructorRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/assignments/1/submissions/student/2"))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).findForStudent(anyLong());
    }
}
