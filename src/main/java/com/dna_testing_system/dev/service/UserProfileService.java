package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.dto.response.profile.ProfileResponse;

import java.util.List;

public interface UserProfileService {
    /**
     * Creates a new user profile based on the provided request data.
     *
     * @param request the user profile request containing the necessary information
     * @return true if the user profile was created successfully, false otherwise
     */
    boolean updateUserProfile(String username, UserProfileRequest request);


    UserProfileResponse getUserProfile(String username);

        /**
        * Deletes the user profile associated with the given username.
        *
        * @param username the username of the user whose profile is to be deleted
        * @return true if the user profile was deleted successfully, false otherwise
        */
    boolean deleteUserProfile(String username);
    /**
     * Checks if the user profile exists for the given username.
     *
     * @param name the name of the user
     * @return true if the user profile exists, false otherwise
     */
    List<UserProfileResponse> getUserProfileByName(String name);

    List<UserProfileResponse> getUserProfiles();

    ProfileResponse getProfile(String username);

}
