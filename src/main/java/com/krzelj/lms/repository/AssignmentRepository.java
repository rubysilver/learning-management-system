package com.krzelj.lms.repository;

import com.krzelj.lms.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByDueAtAsc(Long courseId);

    @Query("select a from Assignment a left join fetch a.course where a.id = :id")
    java.util.Optional<Assignment> findByIdWithCourse(@Param("id") Long id);

    @Query("""
            select a
            from Assignment a
            where a.dueAt between :from and :to
            order by a.dueAt asc
            """)
    List<Assignment> findDueBetween(@Param("from") Instant from, @Param("to") Instant to);
}

