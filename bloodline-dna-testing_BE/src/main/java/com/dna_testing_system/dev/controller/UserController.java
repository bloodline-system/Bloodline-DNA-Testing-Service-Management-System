package com.dna_testing_system.dev.controller;


import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.service.ContentPostService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/profiles")
public class UserController {

    UserProfileService userProfileService;
    OrderService orderService;
    OrderKitService orderKitService;
    OrderParticipantService orderParticipantService;
    UserService userService;
    StaffService staffService;
    MedicalServiceManageService medicalService;
    TestKitService testKitService;

    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllProfiles() {
        List<UserProfileResponse> profiles = userProfileService.getUserProfiles();
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Get all profiles successfully", profiles)
        );
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> searchProfiles(@RequestParam("name") String name) {
        List<UserProfileResponse> profiles = userProfileService.getUserProfileByName(name);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Search profiles successfully", profiles)
        );
    }

    @GetMapping("/{username}")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable String username,
                                                                       HttpServletRequest httpServletRequest) {
        try {
            UserProfileResponse userProfile = userProfileService.getUserProfile(username);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Get profile successfully", userProfile)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", httpServletRequest.getRequestURI()));
        }
    }

    @PutMapping("/{username}")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@PathVariable String username,
                                                                          @ModelAttribute("userEditProfile") UserProfileRequest userProfile,
                                                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                                                          HttpServletRequest httpServletRequest) {
        UserProfileResponse existingProfile = userProfileService.getUserProfile(username);



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


        boolean updated = userProfileService.updateUserProfile(username, userProfile);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", httpServletRequest.getRequestURI()));
        }

        UserProfileResponse updatedProfile = userProfileService.getUserProfile(username);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Update profile successfully", updatedProfile)
        );
    }

    @DeleteMapping("/{username}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Boolean>> deleteProfile(@PathVariable String username,
                                                              HttpServletRequest httpServletRequest) {
        boolean deleted = userProfileService.deleteUserProfile(username);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", httpServletRequest.getRequestURI()));
        }

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Delete profile successfully", true)
        );
    }
    @GetMapping("/dashboard")
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
