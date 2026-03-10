package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.entity.User;

/**
 * Service for user management and profile operations
 */
public interface UserService {
    /**
     * Get a user by ID
     * @param userId The ID of the user to retrieve
     * @return The user entity
     * @throws ResourceNotFoundException if the user is not found
     */
    User getUserById(String userId);
    
    /**
     * Update a user's profile with additional information
     * @param userId The ID of the user to update
     * @param request The profile update request with fields to update
     * @return The updated user entity
     */
    User updateProfile(String userId, UpdateProfileRequest request);
    
    /**
     * Convert a User entity to UserResponse DTO
     * @param user The user entity to convert
     * @return UserResponse DTO
     */
    UserResponse toUserResponse(User user);

    TestResult getTestResult(Long orderId);
}
