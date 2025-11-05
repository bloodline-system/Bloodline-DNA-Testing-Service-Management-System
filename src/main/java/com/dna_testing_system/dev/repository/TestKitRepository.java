package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.TestKit;
import com.dna_testing_system.dev.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestKitRepository extends JpaRepository<TestKit, Long> {

}
