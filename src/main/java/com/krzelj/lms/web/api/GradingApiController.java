package com.krzelj.lms.web.api;

import com.krzelj.lms.repository.jdbc.GradeImportJdbcRepository;
import com.krzelj.lms.repository.jdbc.ReportingJdbcRepository;
import com.krzelj.lms.repository.jdbc.dto.AssignmentGradeReportRow;
import com.krzelj.lms.repository.jdbc.dto.GradeImportRow;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/grading")
public class GradingApiController {

    private final GradeImportJdbcRepository gradeImportJdbcRepository;
    private final ReportingJdbcRepository reportingJdbcRepository;

    public GradingApiController(GradeImportJdbcRepository gradeImportJdbcRepository,
                                ReportingJdbcRepository reportingJdbcRepository) {
        this.gradeImportJdbcRepository = gradeImportJdbcRepository;
        this.reportingJdbcRepository = reportingJdbcRepository;
    }

    @PostMapping("/bulk-import")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<BulkImportResponse> bulkImportGrades(
            @RequestParam Long graderUserId,
            @Valid @RequestBody BulkGradeImportRequest request) {

        int[][] updateCounts = gradeImportJdbcRepository.batchUpdateGrades(
                request.grades(),
                graderUserId,
                Instant.now()
        );

        int totalUpdated = 0;
        for (int[] batch : updateCounts) {
            for (int count : batch) {
                totalUpdated += count;
            }
        }

        return ResponseEntity.ok(new BulkImportResponse(totalUpdated, request.grades().size()));
    }

    @GetMapping("/report/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<AssignmentGradeReportRow>> getCourseGradeReport(@PathVariable Long courseId) {
        List<AssignmentGradeReportRow> report = reportingJdbcRepository.assignmentGradeReportForCourse(courseId);
        return ResponseEntity.ok(report);
    }

    public record BulkGradeImportRequest(List<GradeImportRow> grades) {
    }

    public record BulkImportResponse(int gradesUpdated, int totalSubmitted) {
    }
}
