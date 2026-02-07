package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private NotificationService notificationService;

    private User testStudent;
    private Assignment testAssignment;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testStudent = new User("student1", "hash", "student@test.com");
        testStudent.setLocale("en");

        testCourse = new Course("CS101", "Introduction to Computer Science", null);
        testCourse.setId(1L);

        testAssignment = new Assignment(testCourse, "Homework 1", Instant.now().plusSeconds(86400));
        testAssignment.setId(1L);
    }

    @Test
    void sendAssignmentDeadlineReminder_Success() {
        String subject = "[LMS] Upcoming deadline in course CS101: Homework 1";
        String body = "Hello student1,\n\nThis is a reminder...";
        
        when(messageSource.getMessage(eq("email.assignment.reminder.subject"), any(), eq(Locale.ENGLISH)))
                .thenReturn(subject);
        when(messageSource.getMessage(eq("email.assignment.reminder.body"), any(), eq(Locale.ENGLISH)))
                .thenReturn(body);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAssignmentDeadlineReminder(testStudent, testAssignment);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("student@test.com", sentMessage.getTo()[0]);
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(body, sentMessage.getText());
    }

    @Test
    void sendAssignmentDeadlineReminder_UsesGermanLocale() {
        testStudent.setLocale("de");
        String subject = "[LMS] Bevorstehende Abgabefrist";
        
        when(messageSource.getMessage(eq("email.assignment.reminder.subject"), any(), eq(Locale.GERMAN)))
                .thenReturn(subject);
        when(messageSource.getMessage(eq("email.assignment.reminder.body"), any(), eq(Locale.GERMAN)))
                .thenReturn("Body");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAssignmentDeadlineReminder(testStudent, testAssignment);

        verify(messageSource).getMessage(eq("email.assignment.reminder.subject"), any(), eq(Locale.GERMAN));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAssignmentDeadlineReminder_UsesDefaultLocaleWhenNull() {
        testStudent.setLocale(null);
        
        when(messageSource.getMessage(anyString(), any(), eq(Locale.ENGLISH)))
                .thenReturn("Test");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAssignmentDeadlineReminder(testStudent, testAssignment);

        verify(messageSource, atLeastOnce()).getMessage(anyString(), any(), eq(Locale.ENGLISH));
    }

    @Test
    void sendAssignmentDeadlineReminder_UsesDefaultLocaleWhenBlank() {
        testStudent.setLocale("  ");
        
        when(messageSource.getMessage(anyString(), any(), eq(Locale.ENGLISH)))
                .thenReturn("Test");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAssignmentDeadlineReminder(testStudent, testAssignment);

        verify(messageSource, atLeastOnce()).getMessage(anyString(), any(), eq(Locale.ENGLISH));
    }
}
