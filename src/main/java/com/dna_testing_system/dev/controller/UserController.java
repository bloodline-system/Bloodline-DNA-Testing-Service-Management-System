package com.dna_testing_system.dev.controller;


import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.dto.response.staff.TestResultsResponse;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.service.ContentPostService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.*;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import com.dna_testing_system.dev.service.staff.StaffService;
import com.dna_testing_system.dev.service.staff.TestKitService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import com.dna_testing_system.dev.service.user.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/profiles")
@SecurityRequirement(name = "bearerAuth")
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @PathVariable String username,
                                                                          @RequestBody UserProfileRequest userProfile,
                                                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                                                          HttpServletRequest httpServletRequest) {
        try {
            UserProfileResponse existingProfile = userProfileService.getUserProfile(username);
            String oldImageUrl = null;

            // Handle file upload and get new image URL (before transaction)
            if (file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                try {
                    String uploadsDir = "uploads/";
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path uploadPath = Paths.get(uploadsDir);
                    
                    Files.createDirectories(uploadPath);
                    Path filePath = uploadPath.resolve(fileName);
                    file.transferTo(filePath.toFile());
                    
                    String newImageUrl = "/uploads/" + fileName;
                    oldImageUrl = existingProfile.getProfileImageUrl();
                    userProfile.setProfileImageUrl(newImageUrl);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "File upload failed: " + e.getMessage(), httpServletRequest.getRequestURI()));
                }
            } else {
                // Keep existing image if no new file uploaded
                userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
            }

            // Preserve dateOfBirth if not provided
            if (userProfile.getDateOfBirth() == null) {
                userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
            }

            // Preserve email if not provided (required field in database)
            if (userProfile.getEmail() == null || userProfile.getEmail().trim().isEmpty()) {
                userProfile.setEmail(existingProfile.getEmail());
            }

            // Update profile in database (transactional)
            boolean updated = userProfileService.updateUserProfile(username, userProfile);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", httpServletRequest.getRequestURI()));
            }

            // Delete old image file AFTER successful transaction (non-transactional cleanup)
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                deleteOldProfileImage(oldImageUrl);
            }

            UserProfileResponse updatedProfile = userProfileService.getUserProfile(username);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Update profile successfully", updatedProfile)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating profile: " + e.getMessage(), httpServletRequest.getRequestURI()));
        }
    }

    /**
     * Delete old profile image file asynchronously (non-transactional cleanup)
     * Failures in this operation do not affect the main transaction
     */
    private void deleteOldProfileImage(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("/uploads/")) {
                // Convert URL path to file system path
                String fileSystemPath = imageUrl.substring(1); // Remove leading slash: "/uploads/file.jpg" -> "uploads/file.jpg"
                Path imagePath = Paths.get(fileSystemPath);
                Files.deleteIfExists(imagePath);
            }
        } catch (Exception e) {
            // Log the error but don't fail the request - old file deletion is not critical
            System.err.println("Warning: Failed to delete old profile image: " + e.getMessage());
        }
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
}
