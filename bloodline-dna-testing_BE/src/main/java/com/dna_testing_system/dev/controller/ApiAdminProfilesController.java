package com.dna_testing_system.dev.controller;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiAdminProfilesController {
    UserProfileService userProfileService;


    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<List<UserProfileResponse>> getAllProfiles() {
        return ResponseEntity.ok(userProfileService.getUserProfiles());
    }

//    @GetMapping("/manage/profile")
//    public String getProfileByUsername(@RequestParam("username") String username, Model model) {
//        UserProfileResponse userProfile = userProfileService.getUserProfile(username);
//        model.addAttribute("userProfile", userProfile);
//        return "admin-profiles"; // Return the view name for the specific profile page
//    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProfiles(@RequestParam(required = false) String query) {
        if (query == null || query.isEmpty()) {
            return ResponseEntity.ok(userProfileService.getUserProfiles());
        }

        UserProfileResponse userProfile = userProfileService.getUserProfile(query);
        if (userProfile == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userProfile);
    }



    // Show update profile form by username
    @GetMapping("{username}")
    public ResponseEntity<?> getProfileByUsername(@PathVariable String username) {
        UserProfileResponse userProfile = userProfileService.getUserProfile(username);
        if (userProfile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userProfile);
    }



    // Update profile by username
    @PutMapping("{username}")
    public ResponseEntity<?> updateProfile(
            @PathVariable String username,
            @RequestPart("profile") UserProfileRequest userProfile,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        UserProfileResponse existingProfile = userProfileService.getUserProfile(username);
        if (existingProfile == null) {
            return ResponseEntity.notFound().build();
        }

        // Xử lý ảnh
        if (file == null || file.isEmpty()) {
            userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
        } else {
            try {
                String uploadsDir = "uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadsDir + fileName);
                Files.createDirectories(Paths.get(uploadsDir));
                file.transferTo(path);
                userProfile.setProfileImageUrl("/uploads/" + fileName);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Upload ảnh thất bại: " + e.getMessage());
            }
        }

        // Xử lý ngày sinh
        if (userProfile.getDateOfBirth() == null) {
            userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
        }

        userProfileService.updateUserProfile(username, userProfile);
        return ResponseEntity.ok("Cập nhật profile thành công");
    }



    //delete profile by username
    @DeleteMapping("{username}")
    public ResponseEntity<?> deleteProfile(@PathVariable String username) {
        UserProfileResponse existingProfile = userProfileService.getUserProfile(username);
        if (existingProfile == null) {
            return ResponseEntity.notFound().build();
        }
        userProfileService.deleteUserProfile(username);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
