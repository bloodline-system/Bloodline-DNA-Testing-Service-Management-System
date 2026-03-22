package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
