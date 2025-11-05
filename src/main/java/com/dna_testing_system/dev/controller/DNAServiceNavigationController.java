package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DNAServiceNavigationController {

    UserProfileService userProfileService;

    /**
     * Hiển thị trang chọn dịch vụ DNA
     * Đây là trang để user chọn các service cụ thể (được truy cập từ /user/home)
     */
    @GetMapping("/user/dashboard")
    public String showServiceDashboard(Model model) {
        // Lấy thông tin user đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }

        return "user/dashboard"; // trả về template dashboard.html (service selection page)
    }
}