package com.krzelj.lms.service;

import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.CourseRepository;
import com.krzelj.lms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAllWithInstructor();
    }

    @Transactional(readOnly = true)
    public Course getById(Long id) {
        Course course = courseRepository.findByIdWithInstructorAndStudents(id)
                .orElseGet(() -> courseRepository.findByIdWithInstructor(id)
                        .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id)));

        if (course.getInstructor() != null) {
            course.getInstructor().getUsername();
            course.getInstructor().getId();
        }
        if (course.getStudents() != null) {
            course.getStudents().size();
            course.getStudents().forEach(student -> {
                student.getId();
                student.getUsername();
            });
        }
        
        return course;
    }

    @Transactional(readOnly = true)
    public List<Course> findForInstructor(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    @Transactional(readOnly = true)
    public List<Course> findForStudent(Long studentId) {
        return courseRepository.findCoursesForStudent(studentId);
    }

    public Course createCourse(String code, String title, String description, Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("Instructor not found: " + instructorId));
        Course course = new Course(code, title, instructor);
        course.setDescription(description);
        return courseRepository.save(course);
    }

    public Course save(Course course) {
        return courseRepository.save(course);
    }

    public void delete(Long id) {
        courseRepository.deleteById(id);
    }

    public void enrollStudent(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        
        if (course.getStudents().contains(student)) {
            throw new IllegalArgumentException("Student is already enrolled in this course");
        }
        
        course.getStudents().add(student);
        courseRepository.save(course);
    }

    public void unenrollStudent(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        
        course.getStudents().remove(student);
        courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long courseId, Long studentId) {
        Course course = courseRepository.findByIdWithInstructorAndStudents(courseId)
                .orElseGet(() -> courseRepository.findByIdWithInstructor(courseId)
                        .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId)));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        return course.getStudents() != null && course.getStudents().contains(student);
    }
}

