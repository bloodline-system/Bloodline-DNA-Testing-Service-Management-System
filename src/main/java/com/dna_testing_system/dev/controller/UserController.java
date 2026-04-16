package com.dna_testing_system.dev.controller;


import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.dto.response.profile.ProfileResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final String UPLOAD_API_PREFIX = "/api/upload/files/";
    private static final String UPLOAD_STATIC_PREFIX = "/uploads/";
    private static final Pattern SAFE_UPLOAD_FILE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    UserProfileService userProfileService;
    UploadImageService uploadImageService;
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

    @GetMapping("/me")
    @ResponseBody
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(HttpServletRequest request) {
        try {
            String currentUsername = currentUsername();
            ProfileResponse userProfile = userProfileService.getProfile(currentUsername);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Get my profile successfully", userProfile)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
        }
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

    @PutMapping(value = "/{username}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfileJson(@PathVariable String username,
                                                                              @Valid @RequestBody UserProfileRequest userProfile,
                                                                              HttpServletRequest httpServletRequest) {
        return updateProfileInternal(username, userProfile, null, httpServletRequest);
    }

    @PutMapping(value = "/{username}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfileMultipart(@PathVariable String username,
                                                                                   @Valid @ModelAttribute UserProfileRequest userProfile,
                                                                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                                                                   HttpServletRequest httpServletRequest) {
        return updateProfileInternal(username, userProfile, file, httpServletRequest);
    }

    private ResponseEntity<ApiResponse<UserProfileResponse>> updateProfileInternal(String username,
                                                                                   UserProfileRequest userProfile,
                                                                                   MultipartFile file,
                                                                                   HttpServletRequest httpServletRequest) {
        try {
            UserProfileResponse existingProfile = userProfileService.getUserProfile(username);
            String oldImageUrl = existingProfile.getProfileImageUrl();

            boolean hasNewFile = file != null && !file.isEmpty();
            String requestedImageUrl = userProfile.getProfileImageUrl();
            boolean hasRequestedImageUrl =
                    requestedImageUrl != null && !requestedImageUrl.trim().isEmpty();

            if (hasNewFile) {
                try {
                    String generatedFileName = uploadImageService.saveImage(file);
                    userProfile.setProfileImageUrl(UPLOAD_API_PREFIX + generatedFileName);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "File upload failed: " + e.getMessage(), httpServletRequest.getRequestURI()));
                }
            } else if (hasRequestedImageUrl) {
                String normalizedImageUrl = normalizeProfileImageUrl(requestedImageUrl);
                if (normalizedImageUrl == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid profileImageUrl", httpServletRequest.getRequestURI()));
                }
                userProfile.setProfileImageUrl(normalizedImageUrl);
            } else {
                userProfile.setProfileImageUrl(oldImageUrl);
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

            UserProfileResponse updatedProfile = userProfileService.getUserProfile(username);

            // Delete old image file AFTER successful transaction (non-transactional cleanup)
            String newImageUrl = updatedProfile.getProfileImageUrl();
            if (oldImageUrl != null && !oldImageUrl.isBlank()
                    && newImageUrl != null && !newImageUrl.isBlank()
                    && !oldImageUrl.equals(newImageUrl)) {
                deleteOldProfileImage(oldImageUrl);
            }

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Update profile successfully", updatedProfile)
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), httpServletRequest.getRequestURI()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), httpServletRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating profile: " + e.getMessage(), httpServletRequest.getRequestURI()));
        }
    }

    private String normalizeProfileImageUrl(String rawUrl) {
        String fileName = extractUploadedFileName(rawUrl);
        if (fileName == null) return null;

        Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
        Path imagePath = uploadsDir.resolve(fileName).normalize();
        if (!imagePath.startsWith(uploadsDir)) return null;
        if (!Files.exists(imagePath)) return null;

        return UPLOAD_API_PREFIX + fileName;
    }

    private String extractUploadedFileName(String imageUrl) {
        if (imageUrl == null) return null;

        String trimmed = imageUrl.trim();
        if (trimmed.isEmpty()) return null;

        String fileName;
        if (trimmed.startsWith(UPLOAD_API_PREFIX)) {
            fileName = trimmed.substring(UPLOAD_API_PREFIX.length());
        } else if (trimmed.startsWith(UPLOAD_STATIC_PREFIX)) {
            fileName = trimmed.substring(UPLOAD_STATIC_PREFIX.length());
        } else {
            return null;
        }

        if (fileName.isEmpty() || fileName.length() > 255) return null;
        if (fileName.contains("..")) return null;
        if (!SAFE_UPLOAD_FILE_NAME.matcher(fileName).matches()) return null;

        return fileName;
    }

    /**
     * Delete old profile image file asynchronously (non-transactional cleanup)
     * Failures in this operation do not affect the main transaction
     */
    private void deleteOldProfileImage(String imageUrl) {
        try {
            String fileName = extractUploadedFileName(imageUrl);
            if (fileName == null) {
                return;
            }

            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            Path imagePath = uploadsDir.resolve(fileName).normalize();
            if (!imagePath.startsWith(uploadsDir)) {
                return;
            }

            Files.deleteIfExists(imagePath);
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

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
