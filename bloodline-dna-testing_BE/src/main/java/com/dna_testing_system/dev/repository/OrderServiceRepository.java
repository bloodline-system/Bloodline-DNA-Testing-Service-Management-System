package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderServiceRepository extends JpaRepository<ServiceOrder, Long> {
    // Define custom query methods if needed
}
