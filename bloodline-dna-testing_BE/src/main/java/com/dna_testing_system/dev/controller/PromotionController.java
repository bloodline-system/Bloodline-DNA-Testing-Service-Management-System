package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.entity.Promotion;
import com.dna_testing_system.dev.service.PromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/promotions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionController {

    PromotionService promotionService;

    // Hiển thị trang danh sách các voucher (promotion)
    @GetMapping
    public String showPromotionList(Model model) {
        model.addAttribute("promotions", promotionService.findAll());
        return "manager/promotions";
    }

    // Tạo mới một voucher
    @PostMapping("/create")
    public String createPromotion(@ModelAttribute Promotion promotion) {
        promotionService.create(promotion);
        return "redirect:/manager/promotions";
    }

    // Cập nhật thông tin voucher từ form
    @PostMapping("/update")
    public String updatePromotion(@ModelAttribute Promotion promotion) {
        promotionService.update(promotion.getId(), promotion);
        return "redirect:/manager/promotions";
    }

    // Xóa voucher theo ID
    @GetMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id) {
        promotionService.delete(id);
        return "redirect:/manager/promotions";
    }

    // API: Lấy thông tin voucher theo ID (dùng cho fetch AJAX)
    @GetMapping("/{id}")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.findById(id));
    }
}
