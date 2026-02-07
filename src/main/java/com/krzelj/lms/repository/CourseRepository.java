package com.krzelj.lms.repository;

import com.krzelj.lms.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCode(String code);

    @Query("""
            select c
            from Course c
            left join fetch c.instructor
            where c.instructor.id = :instructorId
            """)
    List<Course> findByInstructorId(@Param("instructorId") Long instructorId);

    @Query("""
            select distinct c
            from Course c
            left join fetch c.instructor
            """)
    List<Course> findAllWithInstructor();

    @Query("""
            select c
            from Course c
            left join fetch c.instructor
            where c.id = :id
            """)
    Optional<Course> findByIdWithInstructor(@Param("id") Long id);

    @Query("""
            select c
            from Course c
            left join fetch c.instructor
            left join fetch c.students
            where c.id = :id
            """)
    Optional<Course> findByIdWithInstructorAndStudents(@Param("id") Long id);

    @Query("""
            select c
            from Course c
            left join fetch c.instructor
            join c.students s
            where s.id = :studentId
            """)
    List<Course> findCoursesForStudent(@Param("studentId") Long studentId);
}

