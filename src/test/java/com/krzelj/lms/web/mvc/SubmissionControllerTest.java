package com.krzelj.lms.web.mvc;

import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.jdbc.GradeImportJdbcRepository;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.AssignmentService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubmissionController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GradeImportJdbcRepository gradeImportJdbcRepository;

    @MockitoBean
    private JwtService jwtService;

    private Course testCourse;
    private Assignment testAssignment;
    private User testStudent;
    private User testInstructor;
    private Submission testSubmission;

    @BeforeEach
    void setUp() {
        testInstructor = new User("instructor1", "hash", "inst@test.com");
        testInstructor.setId(1L);
        testInstructor.setRoles(Set.of(new Role(RoleName.INSTRUCTOR)));

        testCourse = new Course("CS101", "Test Course", testInstructor);
        testCourse.setId(1L);

        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plus(7, ChronoUnit.DAYS));
        testAssignment.setId(1L);

        testStudent = new User("student1", "hash", "stud@test.com");
        testStudent.setId(2L);
        testStudent.setRoles(Set.of(new Role(RoleName.STUDENT)));

        testSubmission = new Submission(testAssignment, testStudent);
        testSubmission.setId(1L);
        testSubmission.setContentText("My answer");
        testSubmission.setSubmittedAt(Instant.now());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void listSubmissions_AsInstructor_ReturnsAllSubmissions() throws Exception {
        when(assignmentService.getByIdWithCourse(1L)).thenReturn(testAssignment);
        when(userService.findByUsername("instructor1")).thenReturn(Optional.of(testInstructor));
        when(submissionService.findForAssignmentWithDetails(1L)).thenReturn(List.of(testSubmission));

        mockMvc.perform(get("/assignments/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(view().name("submissions/list"))
                .andExpect(model().attributeExists("assignment"))
                .andExpect(model().attributeExists("submissions"));

        verify(submissionService).findForAssignmentWithDetails(1L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void listSubmissions_AsStudent_ReturnsOwnSubmissionsOnly() throws Exception {
        when(assignmentService.getByIdWithCourse(1L)).thenReturn(testAssignment);
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        when(submissionService.findForAssignmentWithDetails(1L)).thenReturn(List.of(testSubmission));

        mockMvc.perform(get("/assignments/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(view().name("submissions/list"))
                .andExpect(model().attributeExists("submissions"));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void submitWork_AsStudent_RedirectsToCourseAssignment() throws Exception {
        when(userService.findByUsername("student1")).thenReturn(Optional.of(testStudent));
        when(submissionService.submitWork(1L, 2L, "My work")).thenReturn(testSubmission);
        when(assignmentService.getByIdWithCourse(1L)).thenReturn(testAssignment);

        mockMvc.perform(post("/assignments/1/submissions/submit")
                        .param("contentText", "My work")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/assignments/1"));

        verify(submissionService).submitWork(1L, 2L, "My work");
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void gradeSubmission_AsInstructor_RedirectsToSubmissions() throws Exception {
        when(submissionService.gradeSubmission(1L, 1L, 85)).thenReturn(testSubmission);

        mockMvc.perform(post("/assignments/1/submissions/1/grade")
                        .param("graderUserId", "1")
                        .param("points", "85")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/1/submissions"));

        verify(submissionService).gradeSubmission(1L, 1L, 85);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void bulkGrade_WithValidData_RedirectsWithSuccess() throws Exception {
        when(userService.findByUsername("instructor1")).thenReturn(Optional.of(testInstructor));
        when(gradeImportJdbcRepository.batchUpdateGrades(any(), eq(1L), any(Instant.class)))
                .thenReturn(new int[][]{{1}});

        mockMvc.perform(post("/assignments/1/submissions/bulk-grade")
                        .param("bulkGrades", "2,85")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/1/submissions"));

        verify(gradeImportJdbcRepository).batchUpdateGrades(any(), eq(1L), any(Instant.class));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void bulkGrade_WithEmptyData_RedirectsWithError() throws Exception {
        when(userService.findByUsername("instructor1")).thenReturn(Optional.of(testInstructor));

        mockMvc.perform(post("/assignments/1/submissions/bulk-grade")
                        .param("bulkGrades", "")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/1/submissions"));

        verify(gradeImportJdbcRepository, never()).batchUpdateGrades(any(), anyLong(), any());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void gradeSubmission_AsStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/assignments/1/submissions/1/grade")
                        .param("graderUserId", "1")
                        .param("points", "85")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
