package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.ContentPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContentPostRepository extends JpaRepository<ContentPost, Long>, JpaSpecificationExecutor<ContentPost> {
}
