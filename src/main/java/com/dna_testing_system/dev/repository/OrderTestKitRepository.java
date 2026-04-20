package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.OrderKit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTestKitRepository extends JpaRepository<OrderKit, Long> {
}
