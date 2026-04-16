package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    boolean existsByRoleName(String roleName);
    Optional<Role> findByRoleName(String roleName);
    Optional<Role> findRoleByRoleName(String roleName);
}
