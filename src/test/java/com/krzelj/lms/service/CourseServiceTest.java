package com.krzelj.lms.service;

import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.CourseRepository;
import com.krzelj.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private User testInstructor;
    private User testStudent;

    @BeforeEach
    void setUp() {
        testInstructor = new User("instructor1", "hash", "instructor@test.com");
        testInstructor.setId(1L);
        testInstructor.setRoles(Set.of(new Role(RoleName.INSTRUCTOR)));

        testStudent = new User("student1", "hash", "student@test.com");
        testStudent.setId(2L);
        testStudent.setRoles(Set.of(new Role(RoleName.STUDENT)));

        testCourse = new Course("CS101", "Introduction to Computer Science", testInstructor);
        testCourse.setId(1L);
        testCourse.setDescription("Test course");
    }

    @Test
    void findAll_ReturnsListOfCourses() {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findAllWithInstructor()).thenReturn(courses);

        List<Course> result = courseService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseRepository).findAllWithInstructor();
    }

    @Test
    void getById_WhenExists_ReturnsCourse() {
        when(courseRepository.findByIdWithInstructorAndStudents(1L)).thenReturn(Optional.of(testCourse));

        Course result = courseService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(courseRepository).findByIdWithInstructorAndStudents(1L);
    }

    @Test
    void getById_WhenNotExists_ThrowsException() {
        when(courseRepository.findByIdWithInstructorAndStudents(999L)).thenReturn(Optional.empty());
        when(courseRepository.findByIdWithInstructor(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> courseService.getById(999L));
    }

    @Test
    void findForInstructor_ReturnsListOfCourses() {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findByInstructorId(1L)).thenReturn(courses);

        List<Course> result = courseService.findForInstructor(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseRepository).findByInstructorId(1L);
    }

    @Test
    void findForStudent_ReturnsListOfCourses() {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findCoursesForStudent(2L)).thenReturn(courses);

        List<Course> result = courseService.findForStudent(2L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseRepository).findCoursesForStudent(2L);
    }

    @Test
    void createCourse_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testInstructor));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        Course result = courseService.createCourse("CS101", "Title", "Description", 1L);

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_WhenInstructorNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                courseService.createCourse("CS101", "Title", "Description", 999L));
        verify(userRepository).findById(999L);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void enrollStudent_Success() {
        testCourse.setStudents(new HashSet<>());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        courseService.enrollStudent(1L, 2L);

        verify(courseRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void enrollStudent_WhenAlreadyEnrolled_ThrowsException() {
        testCourse.setStudents(new HashSet<>(Arrays.asList(testStudent)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testStudent));

        assertThrows(IllegalArgumentException.class, () -> courseService.enrollStudent(1L, 2L));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void unenrollStudent_Success() {
        testCourse.setStudents(new HashSet<>(Arrays.asList(testStudent)));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        courseService.unenrollStudent(1L, 2L);

        verify(courseRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void isStudentEnrolled_WhenEnrolled_ReturnsTrue() {
        testCourse.setStudents(new HashSet<>(Arrays.asList(testStudent)));
        when(courseRepository.findByIdWithInstructorAndStudents(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testStudent));

        boolean result = courseService.isStudentEnrolled(1L, 2L);

        assertTrue(result);
    }

    @Test
    void isStudentEnrolled_WhenNotEnrolled_ReturnsFalse() {
        testCourse.setStudents(new HashSet<>());
        when(courseRepository.findByIdWithInstructorAndStudents(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testStudent));

        boolean result = courseService.isStudentEnrolled(1L, 2L);

        assertFalse(result);
    }

    @Test
    void delete_Success() {
        doNothing().when(courseRepository).deleteById(1L);

        courseService.delete(1L);

        verify(courseRepository).deleteById(1L);
    }
}
