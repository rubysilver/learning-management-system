package com.krzelj.lms.security;

import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private Role studentRole;
    private Role instructorRole;

    @BeforeEach
    void setUp() {
        studentRole = new Role(RoleName.STUDENT);
        instructorRole = new Role(RoleName.INSTRUCTOR);

        testUser = new User("testuser", "passwordHash", "test@test.com");
        testUser.setId(1L);
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(studentRole, instructorRole));
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        when(userRepository.findWithRolesByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails details = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(details);
        assertEquals("testuser", details.getUsername());
        assertEquals("passwordHash", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR")));

        verify(userRepository).findWithRolesByUsername("testuser");
    }

    @Test
    void loadUserByUsername_WhenUserDisabled_ReturnsDisabledUserDetails() {
        testUser.setEnabled(false);
        when(userRepository.findWithRolesByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails details = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(details);
        assertFalse(details.isEnabled());

        verify(userRepository).findWithRolesByUsername("testuser");
    }

    @Test
    void loadUserByUsername_WhenUserNotExists_ThrowsUsernameNotFoundException() {
        when(userRepository.findWithRolesByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("unknown"));

        verify(userRepository).findWithRolesByUsername("unknown");
    }
}
