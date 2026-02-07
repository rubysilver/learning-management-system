package com.krzelj.lms.repository;

import com.krzelj.lms.domain.Role;
import com.krzelj.lms.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}

