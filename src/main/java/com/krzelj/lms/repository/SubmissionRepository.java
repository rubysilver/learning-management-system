package com.krzelj.lms.repository;

import com.krzelj.lms.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<Submission> findByAssignmentIdOrderBySubmittedAtAsc(Long assignmentId);

    @Query("""
            select s from Submission s
            left join fetch s.student
            left join fetch s.gradedBy
            where s.assignment.id = :assignmentId
            order by s.submittedAt asc
            """)
    List<Submission> findByAssignmentIdWithStudentAndGraderOrderBySubmittedAtAsc(@Param("assignmentId") Long assignmentId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    List<Submission> findByAssignmentIdAndGradePointsIsNull(Long assignmentId);

    @Query("""
            select s
            from Submission s
            where s.assignment.course.id = :courseId
              and s.student.id = :studentId
            order by s.assignment.dueAt asc
            """)
    List<Submission> findForStudentInCourse(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
}

