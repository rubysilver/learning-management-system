package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.AssignmentRepository;
import com.krzelj.lms.repository.CourseRepository;
import com.krzelj.lms.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
public class AssignmentDeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssignmentDeadlineScheduler.class);

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;
    private final long reminderHoursBeforeDeadline;

    public AssignmentDeadlineScheduler(
            AssignmentRepository assignmentRepository,
            CourseRepository courseRepository,
            SubmissionRepository submissionRepository,
            NotificationService notificationService,
            @Value("${app.scheduler.reminder-hours-before-deadline:24}") long reminderHoursBeforeDeadline) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
        this.notificationService = notificationService;
        this.reminderHoursBeforeDeadline = reminderHoursBeforeDeadline;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.assignment-check-fixed-delay-ms:900000}")
    @Transactional(readOnly = true)
    public void checkUpcomingDeadlines() {
        log.info("Starting scheduled check for upcoming assignment deadlines");

        Instant now = Instant.now();
        Instant reminderWindowEnd = now.plus(reminderHoursBeforeDeadline, ChronoUnit.HOURS);

        List<Assignment> upcomingAssignments = assignmentRepository.findDueBetween(now, reminderWindowEnd);

        if (upcomingAssignments.isEmpty()) {
            log.debug("No assignments due within the next {} hours", reminderHoursBeforeDeadline);
            return;
        }

        log.info("Found {} assignments due within the next {} hours", upcomingAssignments.size(), reminderHoursBeforeDeadline);

        int notificationsSent = 0;
        int assignmentsProcessed = 0;

        for (Assignment assignment : upcomingAssignments) {
            try {
                Course course = courseRepository.findByIdWithInstructorAndStudents(assignment.getCourse().getId())
                        .orElse(null);
                if (course == null) {
                    log.warn("Course not found for assignment {}", assignment.getId());
                    continue;
                }
                Set<User> enrolledStudents = course.getStudents();

                if (enrolledStudents.isEmpty()) {
                    log.debug("No enrolled students for assignment {} in course {}", assignment.getId(), course.getCode());
                    continue;
                }

                List<User> students = enrolledStudents.stream()
                        .filter(user -> user.getRoles().stream()
                                .anyMatch(role -> role.getName() == RoleName.STUDENT))
                        .filter(User::isEnabled)
                        .toList();

                for (User student : students) {
                    boolean hasSubmitted = submissionRepository
                            .findByAssignmentIdAndStudentId(assignment.getId(), student.getId())
                            .isPresent();

                    if (!hasSubmitted) {
                        try {
                            notificationService.sendAssignmentDeadlineReminder(student, assignment);
                            notificationsSent++;
                            log.debug("Sent reminder to student {} for assignment {}", student.getUsername(), assignment.getId());
                        } catch (Exception e) {
                            log.error("Failed to send reminder to student {} for assignment {}: {}",
                                    student.getUsername(), assignment.getId(), e.getMessage(), e);
                        }
                    }
                }

                assignmentsProcessed++;
            } catch (Exception e) {
                log.error("Error processing assignment {}: {}", assignment.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed deadline check: processed {} assignments, sent {} reminder notifications",
                assignmentsProcessed, notificationsSent);
    }
}
