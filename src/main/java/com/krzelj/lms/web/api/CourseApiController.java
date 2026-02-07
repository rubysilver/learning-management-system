package com.krzelj.lms.web.api;

import com.krzelj.lms.domain.Course;
import com.krzelj.lms.service.CourseService;
import com.krzelj.lms.web.api.dto.CourseResponse;
import com.krzelj.lms.web.api.dto.CreateCourseRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseApiController {

    private final CourseService courseService;

    public CourseApiController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> listCourses() {
        List<Course> courses = courseService.findAll();
        List<CourseResponse> response = courses.stream()
                .map(CourseResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        Course course = courseService.getById(id);
        return ResponseEntity.ok(CourseResponse.from(course));
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<CourseResponse>> getCoursesForInstructor(@PathVariable Long instructorId) {
        List<Course> courses = courseService.findForInstructor(instructorId);
        List<CourseResponse> response = courses.stream()
                .map(CourseResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<CourseResponse>> getCoursesForStudent(@PathVariable Long studentId) {
        List<Course> courses = courseService.findForStudent(studentId);
        List<CourseResponse> response = courses.stream()
                .map(CourseResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        Course course = courseService.createCourse(
                request.code(),
                request.title(),
                request.description() != null ? request.description() : "",
                request.instructorId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseResponse.from(course));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
