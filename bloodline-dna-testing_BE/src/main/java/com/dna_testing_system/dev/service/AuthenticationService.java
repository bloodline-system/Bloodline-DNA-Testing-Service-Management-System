package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.AuthenticationRequest;
import com.dna_testing_system.dev.dto.request.RegisterRequest;
import com.dna_testing_system.dev.entity.User;

/**
 * Service for handling authentication and user security operations
 */
public interface AuthenticationService {
    
    /**
     * Register a new user with minimal information
     * @param request Registration request with minimal required fields
     * @return The created user entity
     */
    User register(RegisterRequest request);
    
    /**
     * Activate a user account
     * @param userId The ID of the user to activate
     * @return The updated user entity
     */
    User activateUser(String userId);
    
    /**
     * Authenticate a user with the provided credentials
     * @param request Authentication request containing username and password
     * @return The authenticated user
     * @throws IllegalArgumentException if credentials are invalid
     */
    User authenticate(AuthenticationRequest request);
    
    /**
     * Check if a username is available (not already taken)
     * @param username The username to check
     * @return true if the username is available, false otherwise
     */
    boolean isUsernameAvailable(String username);
}
