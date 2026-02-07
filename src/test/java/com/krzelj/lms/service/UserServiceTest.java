package com.krzelj.lms.service;

import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.RoleRepository;
import com.krzelj.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role studentRole;
    private Role instructorRole;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "hash", "test@test.com");
        testUser.setId(1L);
        testUser.setEnabled(true);

        studentRole = new Role(RoleName.STUDENT);
        studentRole.setId(1L);
        instructorRole = new Role(RoleName.INSTRUCTOR);
        instructorRole.setId(2L);
    }

    @Test
    void findAll_ReturnsListOfUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void findById_WhenExists_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void findByUsername_WhenExists_ReturnsUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByEmail_WhenExists_ReturnsUser() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@test.com");

        assertTrue(result.isPresent());
        assertEquals("test@test.com", result.get().getEmail());
        verify(userRepository).findByEmail("test@test.com");
    }

    @Test
    void save_Success() {
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        verify(userRepository).save(testUser);
    }

    @Test
    void delete_Success() {
        doNothing().when(userRepository).deleteById(1L);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void assignRole_Success() {
        testUser.setRoles(new HashSet<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.assignRole(1L, RoleName.STUDENT);

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(roleRepository).findByName(RoleName.STUDENT);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void assignRole_WhenUserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userService.assignRole(999L, RoleName.STUDENT));
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void removeRole_Success() {
        testUser.setRoles(new HashSet<>(Arrays.asList(studentRole)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.removeRole(1L, RoleName.STUDENT);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserWithRoles_Success() {
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createUserWithRoles(testUser, Set.of(RoleName.STUDENT));

        assertNotNull(result);
        verify(roleRepository).findByName(RoleName.STUDENT);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserWithRoles_CreatesRoleIfNotExists() {
        Role newRole = new Role(RoleName.ADMIN);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(newRole);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createUserWithRoles(testUser, Set.of(RoleName.ADMIN));

        assertNotNull(result);
        verify(roleRepository).findByName(RoleName.ADMIN);
        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findInstructors_ReturnsListOfInstructors() {
        User instructor = new User("instructor", "hash", "instructor@test.com");
        instructor.setRoles(Set.of(instructorRole));
        List<User> allUsers = Arrays.asList(testUser, instructor);
        when(userRepository.findAll()).thenReturn(allUsers);
        when(roleRepository.findByName(RoleName.INSTRUCTOR)).thenReturn(Optional.of(instructorRole));

        List<User> result = userService.findInstructors();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("instructor", result.get(0).getUsername());
    }

    @Test
    void findStudents_ReturnsListOfStudents() {
        testUser.setRoles(Set.of(studentRole));
        List<User> allUsers = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(allUsers);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));

        List<User> result = userService.findStudents();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }
}
