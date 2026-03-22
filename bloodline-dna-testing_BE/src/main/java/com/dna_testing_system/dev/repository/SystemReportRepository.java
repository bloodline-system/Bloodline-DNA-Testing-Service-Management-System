package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.SystemReport;
import com.dna_testing_system.dev.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemReportRepository extends JpaRepository<SystemReport, Long> {
    Optional<SystemReport> findByGeneratedByUser(User generatedByUser);
    List<SystemReport> findAllByGeneratedByUser_Id(String generatedByUserId);
}
