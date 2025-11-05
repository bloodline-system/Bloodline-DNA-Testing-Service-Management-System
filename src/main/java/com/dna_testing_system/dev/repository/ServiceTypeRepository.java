package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    boolean existsByTypeName(String typeName);
}
