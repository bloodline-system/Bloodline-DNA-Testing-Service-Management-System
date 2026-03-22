package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role,Long> {
    boolean existsByRoleName(String roleName);
    Role findByRoleName(String roleName);
}
