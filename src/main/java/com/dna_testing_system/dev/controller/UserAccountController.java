package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.request.auth.ChangePasswordRequestDTO;
import com.dna_testing_system.dev.dto.response.profile.ProfileResponse;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    UserProfileService userProfileService;
    AuthenticationService authenticationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(HttpServletRequest request) {
        try {
            String username = currentUsername();
            ProfileResponse profile = userProfileService.getProfile(username);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Get my profile successfully", profile)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody UserProfileRequest userProfile,
            HttpServletRequest request
    ) {
        try {
            String username = currentUsername();
            ProfileResponse existingProfile = userProfileService.getProfile(username);

            if (userProfile.getProfileImageUrl() == null || userProfile.getProfileImageUrl().isBlank()) {
                userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
            }
            if (userProfile.getDateOfBirth() == null) {
                userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
            }
            if (userProfile.getEmail() == null || userProfile.getEmail().isBlank()) {
                userProfile.setEmail(existingProfile.getEmail());
            }

            boolean updated = userProfileService.updateUserProfile(username, userProfile);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Profile not found", request.getRequestURI()));
            }

            ProfileResponse updatedProfile = userProfileService.getProfile(username);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Update profile successfully", updatedProfile)
            );
            } catch (ResourceNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), request.getRequestURI()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating profile: " + e.getMessage(), request.getRequestURI()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            HttpServletRequest httpServletRequest
    ) {
        try {
            authenticationService.changePassword(currentUsername(), request);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully", null)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), httpServletRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to change password: " + e.getMessage(), httpServletRequest.getRequestURI()));
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}