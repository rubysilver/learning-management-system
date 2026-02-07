package com.krzelj.lms.repository.jdbc;

import com.krzelj.lms.repository.jdbc.dto.AssignmentGradeReportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportingJdbcRepositoryTest {

    @Autowired
    private ReportingJdbcRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM submissions");
        jdbcTemplate.update("DELETE FROM assignments");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, email, enabled, locale) VALUES (1, 'instructor', 'hash', 'instructor@test.com', true, 'en')");
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, email, enabled, locale) VALUES (2, 'student1', 'hash', 'student1@test.com', true, 'en')");
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, email, enabled, locale) VALUES (3, 'student2', 'hash', 'student2@test.com', true, 'en')");
        jdbcTemplate.update("INSERT INTO courses (id, code, title, description, instructor_id, created_at) VALUES (1, 'CS101', 'Test Course', 'Description', 1, ?)", Instant.now());
        jdbcTemplate.update("INSERT INTO assignments (id, course_id, title, description, due_at, max_points) VALUES (1, 1, 'Assignment 1', 'Desc', ?, 100)", Instant.now().plusSeconds(86400));
        jdbcTemplate.update("INSERT INTO assignments (id, course_id, title, description, due_at, max_points) VALUES (2, 1, 'Assignment 2', 'Desc', ?, 100)", Instant.now().plusSeconds(86400));
        jdbcTemplate.update("INSERT INTO submissions (id, assignment_id, student_id, content_text, submitted_at, grade_points) VALUES (1, 1, 2, 'Submission 1', ?, 85)", Instant.now());
        jdbcTemplate.update("INSERT INTO submissions (id, assignment_id, student_id, content_text, submitted_at, grade_points) VALUES (2, 1, 3, 'Submission 2', ?, 90)", Instant.now());
        jdbcTemplate.update("INSERT INTO submissions (id, assignment_id, student_id, content_text, submitted_at) VALUES (3, 2, 2, 'Submission 3', ?)", Instant.now());
    }

    @Test
    void assignmentGradeReportForCourse_ReturnsCorrectReport() {
        List<AssignmentGradeReportRow> report = repository.assignmentGradeReportForCourse(1L);

        assertNotNull(report);
        assertEquals(2, report.size());
        
        AssignmentGradeReportRow assignment1 = report.stream()
                .filter(r -> r.assignmentId() == 1L)
                .findFirst()
                .orElseThrow();
        
        assertEquals(1L, assignment1.assignmentId());
        assertEquals(1L, assignment1.courseId());
        assertEquals(2L, assignment1.submissionsCount());
        assertEquals(2L, assignment1.gradedCount());
        assertEquals(87.5, assignment1.averagePoints(), 0.1);
        
        AssignmentGradeReportRow assignment2 = report.stream()
                .filter(r -> r.assignmentId() == 2L)
                .findFirst()
                .orElseThrow();
        
        assertEquals(2L, assignment2.assignmentId());
        assertEquals(1L, assignment2.submissionsCount());
        assertEquals(0L, assignment2.gradedCount());
        assertNull(assignment2.averagePoints());
    }

    @Test
    void assignmentGradeReportForCourse_WithNoSubmissions_ReturnsEmptyReport() {
        jdbcTemplate.update("DELETE FROM submissions");
        jdbcTemplate.update("DELETE FROM assignments");
        jdbcTemplate.update("INSERT INTO assignments (id, course_id, title, description, due_at, max_points) VALUES (3, 1, 'Assignment 3', 'Desc', ?, 100)", Instant.now().plusSeconds(86400));

        List<AssignmentGradeReportRow> report = repository.assignmentGradeReportForCourse(1L);

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals(0L, report.get(0).submissionsCount());
        assertEquals(0L, report.get(0).gradedCount());
    }
}
