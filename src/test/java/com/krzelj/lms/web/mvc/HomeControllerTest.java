package com.krzelj.lms.web.mvc;

import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Course;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.CourseService;
import com.krzelj.lms.web.api.ApiControllerTestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void home_WhenNotAuthenticated_ReturnsHomeViewWithCourses() throws Exception {
        User instructor = new User("inst", "hash", "inst@test.com");
        Course course = new Course("CS101", "Test", instructor);
        when(courseService.findAll()).thenReturn(List.of(course));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("courses"));

        verify(courseService).findAll();
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void home_WhenAuthenticated_RedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(courseService, never()).findAll();
    }
}
