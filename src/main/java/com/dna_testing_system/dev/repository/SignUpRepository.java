package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.enums.SignUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignUpRepository extends JpaRepository<SignUp,String> {
    boolean existsByUsernameAndStatusIn(String username, List<SignUpStatus> statuses);
    boolean existsByEmailAndStatusIn(String email, List<SignUpStatus> statuses);
    Optional<SignUp> findByIdAndStatus(String id, SignUpStatus status);
    Optional<SignUp> findByUsernameAndStatus(String username, SignUpStatus status);
}
