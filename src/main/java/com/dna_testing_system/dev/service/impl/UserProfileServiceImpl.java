package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.UserProfileMapper;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    @Transactional
    public boolean updateUserProfile(String username, UserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        UserProfile profile = user.getProfile();
        
        // Preserve current email BEFORE modifying profile
        String currentEmail = profile != null ? profile.getEmail() : null;
        
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        
        // Preserve email if not provided in request (required field in database)
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            request.setEmail(currentEmail);
        } else {
            // Validate email uniqueness if email is being updated
            String newEmail = request.getEmail().trim();
            
            // Only check uniqueness if email is actually changing
            if (currentEmail == null || !currentEmail.equalsIgnoreCase(newEmail)) {
                // Check if another user already has this email
                boolean emailExists = userProfileRepository.findAll().stream()
                        .filter(up -> up.getEmail() != null)
                        .anyMatch(up -> up.getEmail().equalsIgnoreCase(newEmail) && !up.getUser().getId().equals(user.getId()));
                
                if (emailExists) {
                    throw new RuntimeException("Email already in use by another user");
                }
            }
        }
        
        userProfileMapper.updateUserProfileFromDto(request, profile);
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        return userProfileMapper.toDto(user);
    }

    @Override
    @Transactional
    public boolean deleteUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        if (user.getProfile() != null) {
            userProfileRepository.delete(user.getProfile());
            user.setProfile(null);
            userRepository.save(user);
        }
        // Nếu muốn xóa luôn user thì có thể gọi userRepository.delete(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUserProfileByName(String name) {
        List<UserProfile> profiles = userProfileRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        return profiles.stream()
                .map(profile -> userProfileMapper.toDto(profile.getUser()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUserProfiles() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(userProfileMapper::toDto)
                .collect(Collectors.toList());
    }
}