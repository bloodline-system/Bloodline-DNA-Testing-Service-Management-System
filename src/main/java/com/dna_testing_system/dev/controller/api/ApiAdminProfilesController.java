package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class ApiAdminProfilesController {
    UserProfileService userProfileService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllProfiles(
            @RequestParam(value = "query", required = false) String query,
            HttpServletRequest request) {
        try {
            List<UserProfileResponse> profiles = (query == null || query.isBlank())
                    ? userProfileService.getUserProfiles()
                    : userProfileService.getUserProfileByName(query);

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Profiles loaded successfully", profiles)
            );
        } catch (Exception e) {
            log.error("Error loading profiles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load profiles", request.getRequestURI()));
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileByUsername(
            @PathVariable String username,
            HttpServletRequest request) {
        try {
            UserProfileResponse userProfile = userProfileService.getUserProfile(username);
            if (userProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
            }
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Profile loaded successfully", userProfile)
            );
        } catch (Exception e) {
            log.error("Error loading profile for username {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load profile", request.getRequestURI()));
        }
    }

    @PutMapping("/{username}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @PathVariable String username,
            @RequestPart("profile") UserProfileRequest userProfile,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {

        UserProfileResponse existingProfile = userProfileService.getUserProfile(username);
        if (existingProfile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
        }

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
                log.error("Failed to upload profile image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to upload profile image", request.getRequestURI()));
            }
        }

        if (userProfile.getDateOfBirth() == null) {
            userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
        }

        boolean updated = userProfileService.updateUserProfile(username, userProfile);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
        }

        UserProfileResponse updatedProfile = userProfileService.getUserProfile(username);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Profile updated successfully", updatedProfile)
        );
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Boolean>> deleteProfile(@PathVariable String username,
                                                              HttpServletRequest request) {
        boolean deleted = userProfileService.deleteUserProfile(username);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
        }

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Profile deleted successfully", true)
        );
    }
}
