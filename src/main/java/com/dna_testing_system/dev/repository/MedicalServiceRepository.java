package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.MedicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {
    List<MedicalService> searchAllByServiceNameContaining(String serviceName);
    Page<MedicalService> searchAllByServiceNameContaining(String serviceName, Pageable pageable);
}
