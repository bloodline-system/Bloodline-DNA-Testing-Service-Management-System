package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.entity.Promotion;
import java.util.List;

public interface PromotionService {
    List<Promotion> findAll();
    Promotion findById(Long id);
    Promotion create(Promotion promotion);
    Promotion update(Long id, Promotion promotion);
    void delete(Long id);
}