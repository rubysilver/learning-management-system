package com.krzelj.lms.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.repository.jdbc.GradeImportJdbcRepository;
import com.krzelj.lms.repository.jdbc.ReportingJdbcRepository;
import com.krzelj.lms.repository.jdbc.dto.AssignmentGradeReportRow;
import com.krzelj.lms.repository.jdbc.dto.GradeImportRow;
import com.krzelj.lms.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GradingApiController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@AutoConfigureJsonTesters
@ActiveProfiles("test")
class GradingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeImportJdbcRepository gradeImportJdbcRepository;

    @MockitoBean
    private ReportingJdbcRepository reportingJdbcRepository;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void bulkImportGrades_WithInstructorRole_ReturnsOk() throws Exception {
        List<GradeImportRow> grades = Arrays.asList(
                new GradeImportRow(1L, 2L, 85),
                new GradeImportRow(1L, 3L, 90)
        );
        when(gradeImportJdbcRepository.batchUpdateGrades(any(), eq(1L), any()))
                .thenReturn(new int[][] { { 1 }, { 1 } });

        GradingApiController.BulkGradeImportRequest request =
                new GradingApiController.BulkGradeImportRequest(grades);

        mockMvc.perform(post("/api/grading/bulk-import")
                        .param("graderUserId", "1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gradesUpdated").value(2))
                .andExpect(jsonPath("$.totalSubmitted").value(2));

        verify(gradeImportJdbcRepository).batchUpdateGrades(any(), eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void bulkImportGrades_WithStudentRole_ReturnsForbidden() throws Exception {
        List<GradeImportRow> grades = Arrays.asList(new GradeImportRow(1L, 2L, 85));
        GradingApiController.BulkGradeImportRequest request =
                new GradingApiController.BulkGradeImportRequest(grades);

        mockMvc.perform(post("/api/grading/bulk-import")
                        .param("graderUserId", "1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(gradeImportJdbcRepository, never()).batchUpdateGrades(any(), anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCourseGradeReport_WithAdminRole_ReturnsReport() throws Exception {
        List<AssignmentGradeReportRow> report = Arrays.asList(
                new AssignmentGradeReportRow(1L, 1L, 10, 8, 82.5)
        );
        when(reportingJdbcRepository.assignmentGradeReportForCourse(1L)).thenReturn(report);

        mockMvc.perform(get("/api/grading/report/course/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].assignmentId").value(1))
                .andExpect(jsonPath("$[0].submissionsCount").value(10))
                .andExpect(jsonPath("$[0].gradedCount").value(8));

        verify(reportingJdbcRepository).assignmentGradeReportForCourse(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void getCourseGradeReport_WithInstructorRole_ReturnsReport() throws Exception {
        when(reportingJdbcRepository.assignmentGradeReportForCourse(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/grading/report/course/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(reportingJdbcRepository).assignmentGradeReportForCourse(2L);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getCourseGradeReport_WithStudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/grading/report/course/1"))
                .andExpect(status().isForbidden());

        verify(reportingJdbcRepository, never()).assignmentGradeReportForCourse(anyLong());
    }
}
