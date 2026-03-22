package com.dna_testing_system.dev.controller;


import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.service.ContentPostService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user") // Thêm base path
public class UserController {

    UserProfileService userProfileService;
    OrderService orderService;
    OrderKitService orderKitService;
    OrderParticipantService orderParticipantService;
    UserService userService;
    StaffService staffService;
    MedicalServiceManageService medicalService;
    TestKitService testKitService;

    @GetMapping("/profile")  // URL sẽ là /user/profile
    public String getProfile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", userProfile);
        return "user/profile";
    }

    @GetMapping("/profile/update")
    public String showUpdateProfileForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse userProfileData = userProfileService.getUserProfile(currentPrincipalName);

        // Dữ liệu cũ chỉ có 1 dòng này
        model.addAttribute("userEditProfile", userProfileData);

        model.addAttribute("userProfile", userProfileData);

        return "user/edit-profile"; //
    }
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("userEditProfile") UserProfileRequest userProfile,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                Model model) { // Thêm Model
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse existingProfile = userProfileService.getUserProfile(currentPrincipalName);



        if (file != null && !file.getOriginalFilename().equals("")) {
            String uploadsDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadsDir + fileName);

            try {

                Files.createDirectories(Paths.get(uploadsDir));
                file.transferTo(path);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String imageUrl = "/uploads/" + fileName;
            userProfile.setProfileImageUrl(imageUrl);
            // Nếu imageUrl là /uploads/abcxyz.jpg
            String oldImageUrl = existingProfile.getProfileImageUrl();
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                // Chuyển về đường dẫn vật lý
                String fileSystemPath = oldImageUrl.replaceFirst("/", ""); // "uploads/abcxyz.jpg"
                Path oldImagePath = Paths.get(fileSystemPath);
                try {
                    Files.deleteIfExists(oldImagePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else {
            // Giữ nguyên ảnh cũ nếu không upload ảnh mới
            userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
        }

        // Giữ nguyên dateOfBirth nếu không có thay đổi
        if (userProfile.getDateOfBirth() == null) {
            userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
        }


        userProfileService.updateUserProfile(currentPrincipalName, userProfile);

        // Refresh lại thông tin user cho header
        UserProfileResponse updatedProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", updatedProfile);

        return "redirect:/user/profile";
    }
    @GetMapping("/user/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard - Trang chủ");
        model.addAttribute("breadcrumbActive", "Dashboard");
        model.addAttribute("currentPage", "dashboard"); // Để đánh dấu mục menu active
        return "user/dashboard"; // Trả về template dashboard.html
    }

    @GetMapping("/view-results")
    public String viewResults(Model model, @RequestParam("orderId") Long orderId) {
        TestResult testResult = userService.getTestResult(orderId);
        TestResultsResponse testResultsResponse = staffService.getTestResultById(testResult.getId());
        RawDataResponse rawDataResponse = staffService.getRawDataById(testResult.getRawData().getId());
        model.addAttribute("rawData", rawDataResponse);
        model.addAttribute("testResult", testResultsResponse);
        // Lấy thông tin kết quả xét nghiệm từ service
        return "user/view-results"; // Trả về template view-results.html
    }
    // Blog for user
    ContentPostService contentPostService;
    // Hien thi danh sach bai viet dang co
    @GetMapping(value = "/posts")
    public String showPostList(Model model) {
        model.addAttribute("posts", contentPostService.getAllPosts());
        return "/user/blog";
    }
}
