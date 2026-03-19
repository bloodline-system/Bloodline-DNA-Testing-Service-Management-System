package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.entity.Promotion;
import com.dna_testing_system.dev.repository.PromotionRepository;
import com.dna_testing_system.dev.service.PromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionServiceImpl implements PromotionService {

    PromotionRepository promotionRepository;

    @Override
    public List<Promotion> findAll() {
        return promotionRepository.findAll();
    }

    @Override
    public Promotion findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher có ID: " + id));
    }

    @Override
    public Promotion create(Promotion promotion) {
        promotion.setCurrentUsageCount(0);
        promotion.setCreatedAt(LocalDateTime.now());
        return promotionRepository.save(promotion);
    }

    @Override
    public Promotion update(Long id, Promotion updated) {
        Promotion existing = findById(id);

        existing.setPromotionCode(updated.getPromotionCode());
        existing.setPromotionName(updated.getPromotionName());
        existing.setPromotionDescription(updated.getPromotionDescription());
        existing.setPromotionType(updated.getPromotionType());
        existing.setDiscountValue(updated.getDiscountValue());
        existing.setMinimumOrderAmount(updated.getMinimumOrderAmount());
        existing.setMaximumDiscountAmount(updated.getMaximumDiscountAmount());
        existing.setUsageLimitPerCustomer(updated.getUsageLimitPerCustomer());
        existing.setTotalUsageLimit(updated.getTotalUsageLimit());
        existing.setIsActive(updated.getIsActive());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setUpdatedAt(LocalDateTime.now());

        return promotionRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }
}
