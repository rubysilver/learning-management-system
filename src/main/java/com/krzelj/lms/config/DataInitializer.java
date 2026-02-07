package com.krzelj.lms.config;

import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.repository.RoleRepository;
import com.krzelj.lms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeData(UserRepository userRepository,
                                            RoleRepository roleRepository,
                                            PasswordEncoder passwordEncoder) {
        return args -> {
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role(RoleName.STUDENT));
                roleRepository.save(new Role(RoleName.INSTRUCTOR));
                roleRepository.save(new Role(RoleName.ADMIN));
            }

            if (userRepository.count() > 0) {
                return;
            }

            Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                    .orElseThrow(() -> new RuntimeException("STUDENT role not found"));
            Role instructorRole = roleRepository.findByName(RoleName.INSTRUCTOR)
                    .orElseThrow(() -> new RuntimeException("INSTRUCTOR role not found"));
            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            User student = new User();
            student.setUsername("student");
            student.setEmail("student@lms.com");
            student.setPasswordHash(passwordEncoder.encode("student123"));
            student.setEnabled(true);
            student.setRoles(Set.of(studentRole));
            userRepository.save(student);

            User instructor = new User();
            instructor.setUsername("instructor");
            instructor.setEmail("instructor@lms.com");
            instructor.setPasswordHash(passwordEncoder.encode("instructor123"));
            instructor.setEnabled(true);
            instructor.setRoles(Set.of(instructorRole));
            userRepository.save(instructor);

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@lms.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);

            User student2 = new User();
            student2.setUsername("john_doe");
            student2.setEmail("john.doe@lms.com");
            student2.setPasswordHash(passwordEncoder.encode("password"));
            student2.setEnabled(true);
            student2.setRoles(Set.of(studentRole));
            userRepository.save(student2);

            User instructor2 = new User();
            instructor2.setUsername("dr_smith");
            instructor2.setEmail("dr.smith@lms.com");
            instructor2.setPasswordHash(passwordEncoder.encode("password"));
            instructor2.setEnabled(true);
            instructor2.setRoles(Set.of(instructorRole));
            userRepository.save(instructor2);
        };
    }
}
