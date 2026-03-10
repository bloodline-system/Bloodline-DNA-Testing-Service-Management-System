package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("""
    SELECT u 
    FROM User u 
    JOIN u.userRoles ur 
    JOIN ur.role r 
    WHERE r.roleName = :roleName
""")
    List<User> findUsersByRoleName(@Param("roleName") String roleName);

    User findUsersByUsername(String username);
}
