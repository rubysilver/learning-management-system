package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.AssignmentRepository;
import com.krzelj.lms.repository.SubmissionRepository;
import com.krzelj.lms.repository.UserRepository;
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
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubmissionService submissionService;

    private Assignment testAssignment;
    private User testStudent;
    private User testGrader;
    private Submission testSubmission;

    @BeforeEach
    void setUp() {
        Course testCourse = new Course("CS101", "Test Course", null);
        testCourse.setId(1L);
        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plusSeconds(86400));
        testAssignment.setId(1L);

        testStudent = new User("student1", "hash", "student@test.com");
        testStudent.setId(1L);

        testGrader = new User("instructor1", "hash", "instructor@test.com");
        testGrader.setId(2L);

        testSubmission = new Submission(testAssignment, testStudent);
        testSubmission.setId(1L);
        testSubmission.setContentText("My submission");
        testSubmission.setSubmittedAt(Instant.now());
    }

    @Test
    void getById_WhenExists_ReturnsSubmission() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));

        Submission result = submissionService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(submissionRepository).findById(1L);
    }

    @Test
    void getById_WhenNotExists_ThrowsException() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> submissionService.getById(999L));
        verify(submissionRepository).findById(999L);
    }

    @Test
    void findForAssignment_ReturnsListOfSubmissions() {
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(1L)).thenReturn(submissions);

        List<Submission> result = submissionService.findForAssignment(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(submissionRepository).findByAssignmentIdOrderBySubmittedAtAsc(1L);
    }

    @Test
    void findForStudent_ReturnsListOfSubmissions() {
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(1L)).thenReturn(submissions);

        List<Submission> result = submissionService.findForStudent(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(submissionRepository).findByStudentIdOrderBySubmittedAtDesc(1L);
    }

    @Test
    void findByAssignmentAndStudent_WhenExists_ReturnsSubmission() {
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.of(testSubmission));

        Optional<Submission> result = submissionService.findByAssignmentAndStudent(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(submissionRepository).findByAssignmentIdAndStudentId(1L, 1L);
    }

    @Test
    void submitWork_NewSubmission_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        Submission result = submissionService.submitWork(1L, 1L, "New content");

        assertNotNull(result);
        verify(assignmentRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void submitWork_UpdateExistingSubmission_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        Submission result = submissionService.submitWork(1L, 1L, "Updated content");

        assertNotNull(result);
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void submitWork_WhenAssignmentNotFound_ThrowsException() {
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                submissionService.submitWork(999L, 1L, "Content"));
        verify(assignmentRepository).findById(999L);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void gradeSubmission_Success() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testGrader));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        Submission result = submissionService.gradeSubmission(1L, 2L, 85);

        assertNotNull(result);
        assertEquals(85, result.getGradePoints());
        assertNotNull(result.getGradedAt());
        assertEquals(testGrader, result.getGradedBy());
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void gradeSubmission_WhenSubmissionNotFound_ThrowsException() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                submissionService.gradeSubmission(999L, 2L, 85));
        verify(submissionRepository).findById(999L);
        verify(submissionRepository, never()).save(any());
    }
}
