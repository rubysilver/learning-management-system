package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.repository.AssignmentRepository;
import com.krzelj.lms.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, CourseRepository courseRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Assignment getById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    @Transactional(readOnly = true)
    public Assignment getByIdWithCourse(Long id) {
        return assignmentRepository.findByIdWithCourse(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Assignment> findForCourse(Long courseId) {
        return assignmentRepository.findByCourseIdOrderByDueAtAsc(courseId);
    }

    public Assignment createAssignment(Long courseId, String title, String description, Instant dueAt, int maxPoints) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        Assignment assignment = new Assignment(course, title, dueAt);
        assignment.setDescription(description);
        assignment.setMaxPoints(maxPoints);
        return assignmentRepository.save(assignment);
    }

    public Assignment save(Assignment assignment) {
        return assignmentRepository.save(assignment);
    }

    public void delete(Long id) {
        assignmentRepository.deleteById(id);
    }
}

