package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.repository.AssignmentRepository;
import com.krzelj.lms.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private Course testCourse;
    private Assignment testAssignment;

    @BeforeEach
    void setUp() {
        testCourse = new Course("CS101", "Introduction to Computer Science", null);
        testCourse.setId(1L);
        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plusSeconds(86400));
        testAssignment.setId(1L);
        testAssignment.setDescription("Complete exercises");
        testAssignment.setMaxPoints(100);
    }

    @Test
    void getById_WhenExists_ReturnsAssignment() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));

        Assignment result = assignmentService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Homework 1", result.getTitle());
        verify(assignmentRepository).findById(1L);
    }

    @Test
    void getById_WhenNotExists_ThrowsException() {
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> assignmentService.getById(999L));
        verify(assignmentRepository).findById(999L);
    }

    @Test
    void getByIdWithCourse_WhenExists_ReturnsAssignment() {
        when(assignmentRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(testAssignment));

        Assignment result = assignmentService.getByIdWithCourse(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(assignmentRepository).findByIdWithCourse(1L);
    }

    @Test
    void findForCourse_ReturnsListOfAssignments() {
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findByCourseIdOrderByDueAtAsc(1L)).thenReturn(assignments);

        List<Assignment> result = assignmentService.findForCourse(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(assignmentRepository).findByCourseIdOrderByDueAtAsc(1L);
    }

    @Test
    void createAssignment_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        Instant dueAt = Instant.now().plusSeconds(86400);
        Assignment result = assignmentService.createAssignment(1L, "New Assignment", "Description", dueAt, 100);

        assertNotNull(result);
        verify(courseRepository).findById(1L);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void createAssignment_WhenCourseNotFound_ThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.createAssignment(999L, "Title", "Desc", Instant.now(), 100));
        verify(courseRepository).findById(999L);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void save_Success() {
        when(assignmentRepository.save(testAssignment)).thenReturn(testAssignment);

        Assignment result = assignmentService.save(testAssignment);

        assertNotNull(result);
        verify(assignmentRepository).save(testAssignment);
    }

    @Test
    void delete_Success() {
        doNothing().when(assignmentRepository).deleteById(1L);

        assignmentService.delete(1L);

        verify(assignmentRepository).deleteById(1L);
    }
}
