package com.krzelj.lms.service;

import com.krzelj.lms.domain.Assignment;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final String fromAddress;
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    public NotificationService(JavaMailSender mailSender,
                               MessageSource messageSource,
                               @Value("${app.notifications.from-address:no-reply@lms.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.fromAddress = fromAddress;
    }

    public void sendAssignmentDeadlineReminder(User student, Assignment assignment) {
        Course course = assignment.getCourse();
        Locale locale = resolveLocale(student);

        String subject = messageSource.getMessage(
                "email.assignment.reminder.subject",
                new Object[]{course.getTitle(), assignment.getTitle()},
                locale
        );

        String dueAtFormatted = dateTimeFormatter.format(assignment.getDueAt());

        String body = messageSource.getMessage(
                "email.assignment.reminder.body",
                new Object[]{student.getUsername(), course.getTitle(), assignment.getTitle(), dueAtFormatted},
                locale
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(student.getEmail());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    private Locale resolveLocale(User user) {
        String languageTag = user.getLocale();
        if (languageTag == null || languageTag.isBlank()) {
            return Locale.ENGLISH;
        }
        return Locale.forLanguageTag(languageTag);
    }
}

