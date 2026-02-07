package com.krzelj.lms.web.mvc;

import com.krzelj.lms.config.SecurityBeansConfig;
import com.krzelj.lms.config.SecurityConfig;
import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.security.jwt.JwtService;
import com.krzelj.lms.service.UserService;
import com.krzelj.lms.web.api.ApiControllerTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityBeansConfig.class, ApiControllerTestSecurityConfig.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("student1", "hash", "student@test.com");
        testUser.setId(1L);
        testUser.setRoles(Set.of(new Role(RoleName.STUDENT)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_AsAdmin_ReturnsListView() throws Exception {
        when(userService.findAll()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"));

        verify(userService).findAll();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listUsers_AsStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newUserForm_ReturnsNewView() throws Exception {
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/new"))
                .andExpect(model().attributeExists("allRoles"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Success_RedirectsToList() throws Exception {
        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userService.createUserWithRoles(any(User.class), any())).thenReturn(testUser);

        mockMvc.perform(post("/admin/users")
                        .param("username", "newuser")
                        .param("email", "new@test.com")
                        .param("password", "password123")
                        .param("roles", "STUDENT")
                        .param("enabled", "true")
                        .param("locale", "en")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).createUserWithRoles(any(User.class), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_DuplicateUsername_RedirectsWithError() throws Exception {
        when(userService.findByUsername("existing")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/admin/users")
                        .param("username", "existing")
                        .param("email", "new@test.com")
                        .param("password", "password123")
                        .param("enabled", "true")
                        .param("locale", "en")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/new"));

        verify(userService, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editUserForm_ReturnsEditView() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/admin/users/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("allRoles"));

        verify(userService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_Success_RedirectsToList() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.findByUsername("updated")).thenReturn(Optional.empty());
        when(userService.findByEmail("updated@test.com")).thenReturn(Optional.empty());
        when(userService.updateUserRoles(any(User.class), any())).thenReturn(testUser);
        when(userService.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "updated")
                        .param("email", "updated@test.com")
                        .param("roles", "STUDENT")
                        .param("enabled", "true")
                        .param("locale", "en")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).save(any(User.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success_RedirectsToList() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(post("/admin/users/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).delete(1L);
    }
}
