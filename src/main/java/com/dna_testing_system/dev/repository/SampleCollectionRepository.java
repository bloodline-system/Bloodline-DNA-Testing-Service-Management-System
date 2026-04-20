package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.SampleCollection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleCollectionRepository extends JpaRepository<SampleCollection,Long> {
    boolean existsByStaff_Id(String staffId);


}
