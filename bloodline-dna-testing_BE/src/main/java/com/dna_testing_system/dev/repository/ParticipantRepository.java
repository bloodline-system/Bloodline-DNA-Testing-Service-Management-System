package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.OrderParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<OrderParticipant, Long> {
    // Additional query methods can be defined here if needed
}
