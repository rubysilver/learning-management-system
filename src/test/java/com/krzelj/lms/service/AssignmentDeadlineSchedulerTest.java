package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.Submission;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.AssignmentRepository;
import com.krzelj.lms.repository.CourseRepository;
import com.krzelj.lms.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentDeadlineSchedulerTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private NotificationService notificationService;

    private AssignmentDeadlineScheduler scheduler;

    private Assignment testAssignment;
    private Course testCourse;
    private User testStudent;
    private User testInstructor;

    @BeforeEach
    void setUp() {
        scheduler = new AssignmentDeadlineScheduler(
                assignmentRepository,
                courseRepository,
                submissionRepository,
                notificationService,
                24L
        );
        
        testInstructor = new User("instructor1", "hash", "instructor@test.com");
        testInstructor.setId(1L);
        testInstructor.setRoles(Set.of(new Role(RoleName.INSTRUCTOR)));

        testStudent = new User("student1", "hash", "student@test.com");
        testStudent.setId(2L);
        testStudent.setRoles(Set.of(new Role(RoleName.STUDENT)));
        testStudent.setEnabled(true);

        testCourse = new Course("CS101", "Introduction to Computer Science", testInstructor);
        testCourse.setId(1L);
        testCourse.setStudents(new HashSet<>(Arrays.asList(testStudent)));

        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plusSeconds(3600));
        testAssignment.setId(1L);
    }

    @Test
    void checkUpcomingDeadlines_NoAssignments_DoesNotSendNotifications() {
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkUpcomingDeadlines();

        verify(assignmentRepository).findDueBetween(any(Instant.class), any(Instant.class));
        verify(notificationService, never()).sendAssignmentDeadlineReminder(any(), any());
    }

    @Test
    void checkUpcomingDeadlines_WithUnsubmittedStudent_SendsNotification() {
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.of(testCourse));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 2L))
                .thenReturn(Optional.empty());
        doNothing().when(notificationService).sendAssignmentDeadlineReminder(any(), any());

        scheduler.checkUpcomingDeadlines();

        verify(notificationService).sendAssignmentDeadlineReminder(testStudent, testAssignment);
    }

    @Test
    void checkUpcomingDeadlines_WithSubmittedStudent_DoesNotSendNotification() {
        Submission submission = new Submission(testAssignment, testStudent);
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.of(testCourse));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 2L))
                .thenReturn(Optional.of(submission));

        scheduler.checkUpcomingDeadlines();

        verify(notificationService, never()).sendAssignmentDeadlineReminder(any(), any());
    }

    @Test
    void checkUpcomingDeadlines_FiltersDisabledStudents() {
        testStudent.setEnabled(false);
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.of(testCourse));

        scheduler.checkUpcomingDeadlines();

        verify(notificationService, never()).sendAssignmentDeadlineReminder(any(), any());
    }

    @Test
    void checkUpcomingDeadlines_FiltersNonStudentUsers() {
        User instructor = new User("instructor2", "hash", "instructor2@test.com");
        instructor.setId(3L);
        instructor.setRoles(Set.of(new Role(RoleName.INSTRUCTOR)));
        instructor.setEnabled(true);
        testCourse.setStudents(new HashSet<>(Arrays.asList(instructor)));

        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.of(testCourse));

        scheduler.checkUpcomingDeadlines();

        verify(notificationService, never()).sendAssignmentDeadlineReminder(any(), any());
    }

    @Test
    void checkUpcomingDeadlines_HandlesNotificationException() {
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.of(testCourse));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 2L))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("Email error"))
                .when(notificationService).sendAssignmentDeadlineReminder(any(), any());

        scheduler.checkUpcomingDeadlines();

        verify(notificationService).sendAssignmentDeadlineReminder(testStudent, testAssignment);
    }

    @Test
    void checkUpcomingDeadlines_HandlesCourseNotFound() {
        List<Assignment> assignments = Arrays.asList(testAssignment);
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.empty());

        scheduler.checkUpcomingDeadlines();

        verify(notificationService, never()).sendAssignmentDeadlineReminder(any(), any());
    }

    @Test
    void checkUpcomingDeadlines_HandlesMultipleAssignments() {
        Assignment assignment2 = new Assignment(testCourse, "Homework 2", Instant.now().plusSeconds(7200));
        assignment2.setId(2L);
        List<Assignment> assignments = Arrays.asList(testAssignment, assignment2);
        
        when(assignmentRepository.findDueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(assignments);
        when(courseRepository.findByIdWithInstructorAndStudents(1L))
                .thenReturn(Optional.of(testCourse));
        when(submissionRepository.findByAssignmentIdAndStudentId(anyLong(), eq(2L)))
                .thenReturn(Optional.empty());

        scheduler.checkUpcomingDeadlines();

        verify(notificationService, times(2)).sendAssignmentDeadlineReminder(eq(testStudent), any());
    }
}
