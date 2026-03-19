package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DNAServiceNavigationController {

    UserProfileService userProfileService;

    /**
     * Hiển thị trang chọn dịch vụ DNA
     * Đây là trang để user chọn các service cụ thể (được truy cập từ /user/home)
     */
    @GetMapping("/dashboard")
    public ApiResponse<UserProfileResponse> showServiceDashboard() {
        // Lấy thông tin user đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            return ApiResponse.success(HttpStatus.OK.value(), "User profile retrieved", userProfile);
        }

        return ApiResponse.success(HttpStatus.OK.value(), "No authenticated user", null);
    }
}
