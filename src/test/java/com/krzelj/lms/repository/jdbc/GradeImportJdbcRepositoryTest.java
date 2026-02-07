package com.krzelj.lms.repository.jdbc;

import com.krzelj.lms.repository.jdbc.dto.GradeImportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GradeImportJdbcRepositoryTest {

    @Autowired
    private GradeImportJdbcRepository repository;

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
        jdbcTemplate.update("INSERT INTO submissions (id, assignment_id, student_id, content_text, submitted_at) VALUES (1, 1, 2, 'Submission 1', ?)", Instant.now());
        jdbcTemplate.update("INSERT INTO submissions (id, assignment_id, student_id, content_text, submitted_at) VALUES (2, 1, 3, 'Submission 2', ?)", Instant.now());
    }

    @Test
    void batchUpdateGrades_UpdatesExistingSubmissions() {
        List<GradeImportRow> rows = Arrays.asList(
                new GradeImportRow(1L, 2L, 85),
                new GradeImportRow(1L, 3L, 90)
        );

        int[][] results = repository.batchUpdateGrades(rows, 1L, Instant.now());

        assertNotNull(results);
        assertEquals(1, results.length);
        Integer grade1 = jdbcTemplate.queryForObject(
                "SELECT grade_points FROM submissions WHERE assignment_id = 1 AND student_id = 2",
                Integer.class);
        Integer grade2 = jdbcTemplate.queryForObject(
                "SELECT grade_points FROM submissions WHERE assignment_id = 1 AND student_id = 3",
                Integer.class);
        
        assertEquals(85, grade1);
        assertEquals(90, grade2);
    }

    @Test
    void batchUpdateGrades_WithNullGrades() {
        List<GradeImportRow> rows = Arrays.asList(
                new GradeImportRow(1L, 2L, null)
        );

        repository.batchUpdateGrades(rows, 1L, Instant.now());

        Integer grade = jdbcTemplate.queryForObject(
                "SELECT grade_points FROM submissions WHERE assignment_id = 1 AND student_id = 2",
                Integer.class);
        
        assertNull(grade);
    }

    @Test
    void batchUpdateGrades_IgnoresNonExistentSubmissions() {
        List<GradeImportRow> rows = Arrays.asList(
                new GradeImportRow(1L, 999L, 100)
        );

        int[][] results = repository.batchUpdateGrades(rows, 1L, Instant.now());

        assertNotNull(results);
    }
}
