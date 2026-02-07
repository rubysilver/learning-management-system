package com.krzelj.lms.repository.jdbc;

import com.krzelj.lms.repository.jdbc.dto.GradeImportRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class GradeImportJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public GradeImportJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int[][] batchUpdateGrades(List<GradeImportRow> rows, long gradedByUserId, Instant gradedAt) {
        String sql = """
                update submissions
                set grade_points = ?,
                    graded_at = ?,
                    graded_by_id = ?
                where assignment_id = ?
                  and student_id = ?
                """;

        Timestamp gradedAtTs = gradedAt == null ? null : Timestamp.from(gradedAt);

        return jdbcTemplate.batchUpdate(sql, rows, 500, (PreparedStatement ps, GradeImportRow row) -> {
            if (row.gradePoints() == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, row.gradePoints());
            }
            if (gradedAtTs == null) {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(2, gradedAtTs);
            }
            ps.setLong(3, gradedByUserId);
            ps.setLong(4, row.assignmentId());
            ps.setLong(5, row.studentId());
        });
    }
}

