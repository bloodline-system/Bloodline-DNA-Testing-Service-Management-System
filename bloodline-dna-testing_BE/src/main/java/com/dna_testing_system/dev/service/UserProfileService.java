package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.dto.response.UserResponse;

import java.util.List;

public interface UserProfileService {
    /**
     * Updates the user profile with the given details.
     *
     *
     * @param firstName the first name of the user
     * @param lastName the last name of the user
     * @param email the email address of the user
     * @param phoneNumber the phone number of the user
     * @param dateOfBirth the date of birth of the user
     * @param profileImageUrl the URL of the user's profile image
     * @param password the password of the user
     * @return true if the update was successful, false otherwise
     */
    boolean updateUserProfile(String username, UserProfileRequest request);

    /**
     * Retrieves the user profile details for the given username.
     *
     * @param id the id of the user
     * @return a string containing the user's profile details
     */
    UserProfileResponse getUserProfile(String username);
    /**
     * Deletes the user profile for the given username.
     *
     * @param id the id of the user
     * @return true if the deletion was successful, false otherwise
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


}
