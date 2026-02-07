package com.krzelj.lms.repository.jdbc;

import com.krzelj.lms.repository.jdbc.dto.AssignmentGradeReportRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReportingJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReportingJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AssignmentGradeReportRow> assignmentGradeReportForCourse(long courseId) {
        String sql = """
                select
                    a.id as assignment_id,
                    a.course_id as course_id,
                    count(s.id) as submissions_count,
                    count(s.grade_points) as graded_count,
                    avg(s.grade_points) as average_points
                from assignments a
                left join submissions s on s.assignment_id = a.id
                where a.course_id = ?
                group by a.id, a.course_id
                order by a.id
                """;

        RowMapper<AssignmentGradeReportRow> mapper = (rs, rowNum) -> new AssignmentGradeReportRow(
                rs.getLong("assignment_id"),
                rs.getLong("course_id"),
                rs.getLong("submissions_count"),
                rs.getLong("graded_count"),
                (Double) rs.getObject("average_points")
        );

        return jdbcTemplate.query(sql, mapper, courseId);
    }
}

